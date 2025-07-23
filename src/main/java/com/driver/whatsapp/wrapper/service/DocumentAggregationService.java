package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.constants.ConfigurationConstants;
import com.driver.whatsapp.wrapper.service.ConfigurationCacheService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service responsible for batching consecutive DOCUMENT / MEDIA type messages from a driver
 * and forwarding them to the Chat-Module as a single message after either:
 *   1. The configurable aggregation window elapses, OR
 *   2. The driver sends a TEXT message, which triggers an immediate flush.
 *
 * Thread-safe and per-driver to avoid crosstalk between different drivers.
 */
@Slf4j
@Service
public class DocumentAggregationService {

    /* ---------------- Dependencies ---------------- */
    @Autowired
    private ChatModuleService chatModuleService;

    @Autowired
    private WhatsAppService whatsAppService;

    /* ---------------- Configuration ---------------- */
    private final long aggregationWindowMs;
    private final long flushBackoffMs;

    /* ---------------- Internal structures ---------------- */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final Map<String, DriverQueue> queues = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> inFlight = new ConcurrentHashMap<>();

    public DocumentAggregationService() {
        // Default values are used if configuration service not yet initialized
        this.aggregationWindowMs = Long.parseLong(
            ConfigurationCacheService.getConfigValue(
                ConfigurationConstants.DOCUMENT_AGGREGATION_WINDOW_MS,
                "15000")
        );
        this.flushBackoffMs = Long.parseLong(
            ConfigurationCacheService.getConfigValue(
                ConfigurationConstants.DOCUMENT_FLUSH_BACKOFF_MS,
                "2000")
        );
        log.info("DocumentAggregationService initialised. aggregationWindowMs={} flushBackoffMs={}", aggregationWindowMs, flushBackoffMs);
    }

    /* ---------------- Public API ---------------- */

    /**
     * Execute an arbitrary task (e.g., sending a TEXT message to Chat-Module) while respecting the same
     * per-driver serialisation guarantee used for document batches. If another task for this driver is
     * already in-flight, the task will be rescheduled after <flushBackoffMs> until the flag becomes free.
     *
     * The task itself is executed asynchronously on the shared scheduler thread-pool, so the caller can
     * return immediately to the webhook.
     */
    public void runSerialised(String driverPhone, Runnable task) {
        AtomicBoolean flag = inFlight.computeIfAbsent(driverPhone, p -> new AtomicBoolean(false));

        if (flag.compareAndSet(false, true)) {
            log.info("Acquired execution slot for driver {}", driverPhone);
            // We acquired the slot – run task asynchronously so we don't block caller thread
            scheduler.execute(() -> {
                try {
                    task.run();
                } catch (Exception ex) {
                    log.error("Serialised task for driver {} failed: {}", driverPhone, ex.getMessage(), ex);
                } finally {
                    flag.set(false);
                    log.info("Released execution slot for driver {}", driverPhone);
                }
            });
        } else {
            log.info("Execution slot busy for driver {} – will retry in {} ms", driverPhone, flushBackoffMs);
            // Someone else (doc flush or previous text) still holds the slot – retry later
            scheduler.schedule(() -> runSerialised(driverPhone, task), flushBackoffMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Enqueue any driver message (DOCUMENT or TEXT) for batching. The batching window is
     * started by the FIRST message that arrives when the queue is empty and is not reset afterwards.
     */
    public void enqueueMessage(DriverMeta meta, String content, String messageId, long timestamp, String messageType, boolean hasUrl) {
        DriverQueue queue = queues.computeIfAbsent(meta.getDriverPhone(), k -> new DriverQueue(meta));

        queue.lock.lock();
        try {
            queue.setMeta(meta); // keep meta fresh
            queue.getItems().add(new QueuedItem(messageType, content, messageId, timestamp));

            boolean firstMessageInBatch = queue.getItems().size() == 1;
            if (firstMessageInBatch) {
                log.info("Started new aggregation batch for driver {} (first {} message)", meta.getDriverPhone(), messageType);
            }

            if (hasUrl) {
                // Each document extends the window: cancel existing task (if any) and start fresh
                if (queue.getFlushTask() != null && !queue.getFlushTask().isDone()) {
                    queue.getFlushTask().cancel(false);
                }

                ScheduledFuture<?> future = scheduler.schedule(
                    () -> flushInternal(meta.getDriverPhone()),
                    aggregationWindowMs,
                    TimeUnit.MILLISECONDS);
                queue.setFlushTask(future);
                log.info("Aggregation timer reset for driver {} due to new document. Next flush in {} ms", meta.getDriverPhone(), aggregationWindowMs);
            }

            log.info("Queued {} message for driver {}. Batch size now {}", messageType, meta.getDriverPhone(), queue.getItems().size());
        } finally {
            queue.lock.unlock();
        }
    }

    /**
     * Returns true if there is an active (non-empty) queue for this driver awaiting flush.
     */
    public boolean isQueueActive(String driverPhone) {
        DriverQueue q = queues.get(driverPhone);
        return q != null && !q.getItems().isEmpty();
    }

    /**
     * Force a flush if there is a pending batch for the driver (e.g., immediately before processing a TEXT message).
     */
    public void flushNow(String driverPhone) {
        flushInternal(driverPhone);
    }

    /* ---------------- Core logic ---------------- */

    private void flushInternal(String driverPhone) {
        DriverQueue queue = queues.get(driverPhone);
        if (queue == null) {
            return; // nothing to flush
        }

        log.info("flushInternal invoked for driver {}. Current queued items: {}", driverPhone, queue.getItems().size());

        queue.lock.lock();
        List<QueuedItem> itemsToFlush;
        DriverMeta meta;
        try {
            if (queue.getItems().isEmpty()) {
                return; // already flushed
            }
            // Cancel scheduled task to avoid double flush
            if (queue.getFlushTask() != null && !queue.getFlushTask().isDone()) {
                queue.getFlushTask().cancel(false);
            }

            itemsToFlush = new ArrayList<>(queue.getItems());
            queue.getItems().clear();
            meta = queue.getMeta();
        } finally {
            queue.lock.unlock();
        }

        // Guard: do not send if Chat-Module is still processing earlier driver message
        AtomicBoolean flag = inFlight.computeIfAbsent(driverPhone, k -> new AtomicBoolean(false));
        if (!flag.compareAndSet(false, true)) {
            log.info("Chat-Module still processing for driver {}. Rescheduling flush in {}ms", driverPhone, flushBackoffMs);
            scheduler.schedule(() -> flushInternal(driverPhone), flushBackoffMs, TimeUnit.MILLISECONDS);
            return;
        }

        try {
            String aggregatedText = buildAggregatedText(itemsToFlush);
            QueuedItem first = itemsToFlush.get(0);

            log.info("Aggregated text for driver {}: {}", driverPhone, aggregatedText);

            log.info("Flushing {} message(s) for driver {} to chat-module", itemsToFlush.size(), driverPhone);

            String aiResponse = chatModuleService.sendMessageToChatModuleWithConfig(
                meta.getConversationId(),
                meta.getDriverPhone(),
                meta.getDriverName(),
                aggregatedText,
                first.getMessageId(),
                first.getTimestamp(),
                "text", // we send as plain text to chat module
                meta.getPlatform(),
                meta.getTenantId(),
                meta.getAssistantId(),
                meta.getLanguageCode()
            );

            log.info("Received AI response after document batch for driver {}: {}", driverPhone, aiResponse);

            if (containsButtonOptions(aiResponse)) {
                ParsedResponse parsed = parseButtonResponse(aiResponse);
                whatsAppService.sendInteractiveButtonMessage(
                    driverPhone,
                    parsed.getBodyText(),
                    parsed.getButtonOptions(),
                    first.getMessageId()
                );
            } else {
                whatsAppService.sendTextMessage(driverPhone, aiResponse, first.getMessageId());
            }
        } catch (Exception e) {
            log.error("Failed to flush documents for driver {}: {}", driverPhone, e.getMessage(), e);
        } finally {
            flag.set(false);
        }
    }

    /* ---------------- Helper methods ---------------- */

    private String buildAggregatedText(List<QueuedItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("Driver sent ").append(items.size()).append(" message(s):\n");
        int index = 1;
        for (QueuedItem it : items) {
            sb.append(index++).append(") [").append(it.getType().toUpperCase()).append("] ").append(it.getContent()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Copied (with minimal changes) from MessageProcessingService to detect button options in AI response.
     */
    private boolean containsButtonOptions(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        String[] lines = response.split("\n");
        if (lines.length < 3) {
            return false;
        }
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("-") || line.matches("^\\s*-\\s+.+")) {
                return true;
            }
        }
        return false;
    }

    private ParsedResponse parseButtonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return new ParsedResponse(response, new ArrayList<>());
        }
        String[] lines = response.split("\n");
        List<String> bodyLines = new ArrayList<>();
        List<String> buttonOptions = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-")) {
                String text = trimmed.replaceFirst("^-\\s*", "").trim();
                if (!text.isEmpty()) {
                    buttonOptions.add(text);
                }
            } else if (!trimmed.isEmpty()) {
                bodyLines.add(trimmed);
            }
        }
        String body = String.join(" ", bodyLines).trim();
        return new ParsedResponse(body, buttonOptions);
    }

    /* ---------------- Inner helper classes ---------------- */

    @Data
    @AllArgsConstructor
    private static class QueuedItem {
        private final String type; // "document" or "text"
        private final String content;
        private final String messageId;
        private final long timestamp;

        public String getType() { return type; }
        public String getContent() { return content; }
        public String getMessageId() { return messageId; }
        public long getTimestamp() { return timestamp; }
    }

    @Data
    @AllArgsConstructor
    public static class DriverMeta {
        private String conversationId;
        private String driverPhone;
        private String driverName;
        private String tenantId;
        private String assistantId;
        private String languageCode;
        private String platform;
    }

    private static class DriverQueue {
        private final List<QueuedItem> items = new ArrayList<>();
        private DriverMeta meta;
        private ScheduledFuture<?> flushTask;
        private final ReentrantLock lock = new ReentrantLock();

        public DriverQueue(DriverMeta meta) {
            this.meta = meta;
        }

        public List<QueuedItem> getItems() { return items; }
        public DriverMeta getMeta() { return meta; }
        public void setMeta(DriverMeta meta) { this.meta = meta; }
        public ScheduledFuture<?> getFlushTask() { return flushTask; }
        public void setFlushTask(ScheduledFuture<?> flushTask) { this.flushTask = flushTask; }
    }

    /* ---------------- Utility model for buttons ---------------- */
    @Data
    @AllArgsConstructor
    private static class ParsedResponse {
        private String bodyText;
        private List<String> buttonOptions;
    }
} 