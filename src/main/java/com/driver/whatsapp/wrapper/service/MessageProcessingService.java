package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.model.WhatsAppWebhookResponse;
import com.driver.whatsapp.wrapper.repository.ApiAssistantMappingRepository;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import com.driver.whatsapp.wrapper.entity.ApiAssistantMapping;
import com.driver.whatsapp.wrapper.entity.ApiAssistantMappingId;
import com.driver.whatsapp.wrapper.entity.TransactionDetail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MessageProcessingService {

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private ChatModuleService chatModuleService;

    @Autowired
    private ApiAssistantMappingRepository apiAssistantMappingRepository;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    // Conversation timeout configuration (30 minutes by default)
    private static final long CONVERSATION_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    // Track last message timestamp for each driver
    private final Map<String, Long> lastMessageTimestamps = new ConcurrentHashMap<>();

    /**
     * Process incoming WhatsApp message from driver using Chat Module
     */
    public void processDriverMessage(WhatsAppWebhookResponse webhookResponse) {
        for (WhatsAppWebhookResponse.Result result : webhookResponse.getResults()) {
            String driverPhone = result.getFrom();
            String messageContent = getMessageContent(result.getMessage());
            String messageId = result.getMessageId();
            String driverName = getContactName(result.getContact());
            String messageType = getMessageType(result.getMessage());
            String platform = getPlatform(result.getIntegrationType());
            long timestamp = parseReceivedAtTimestamp(result.getReceivedAt());

            log.info("Processing message from driver: {} ({}), Content: '{}', Type: {}, Platform: {}, Timestamp: {}", 
                    driverPhone, driverName, messageContent, messageType, platform, timestamp);


            Optional<TransactionDetail> transactionDetail = transactionDetailRepository.findMostRecentOpenConversation(driverPhone);

            if (transactionDetail.isEmpty()) {
                log.warn("No open conversation found for driver: {}. Skipping message processing.", driverPhone);
                return;
            }

            String conversationId = transactionDetail.get().getConversationId();
            String flowName = transactionDetail.get().getFlowName();
            String communicationMode = transactionDetail.get().getCommunicationMode();

            ApiAssistantMapping apiAssistantMapping = apiAssistantMappingRepository.findById(new ApiAssistantMappingId(flowName, communicationMode))
                .orElseThrow(() -> new RuntimeException("Api Assistant Mapping not found for flow: " + flowName + " and mode: " + communicationMode));

            String tenantId = apiAssistantMapping.getTenantId();
            String assistantId = apiAssistantMapping.getAssistantId();
            
            // Check and cleanup expired conversation before processing new message
            cleanupExpiredConversation(driverPhone);

            // Update last message timestamp for this driver
            lastMessageTimestamps.put(driverPhone, System.currentTimeMillis());

            // Skip empty messages
            if (messageContent == null || messageContent.trim().isEmpty()) {
                log.warn("Received empty message from driver {}", driverPhone);
                return;
            }

            try {
                // Send message to chat module and get AI response
                String aiResponse = chatModuleService.sendMessageToChatModuleWithConfig(
                    conversationId,
                    driverPhone, 
                    driverName,
                    messageContent, 
                    messageId, 
                    timestamp,
                    messageType,
                    platform,
                    tenantId,
                    assistantId
                );

                log.info("Received AI response for driver {}: {}", driverPhone, aiResponse);

                // Send AI response back to driver via WhatsApp
                whatsAppService.sendTextMessage(driverPhone, aiResponse, messageId);
                
            } catch (Exception e) {
                log.error("Error processing message from driver {}: {}", driverPhone, e.getMessage(), e);
                
                // Send fallback response
                String fallbackResponse = "I'm having trouble processing your message right now. Please try again or contact support if the issue persists.";
                whatsAppService.sendTextMessage(driverPhone, fallbackResponse, messageId);
            }
        }
    }

    /**
     * Extract message content from webhook response
     */
    private String getMessageContent(WhatsAppWebhookResponse.MessageContent message) {
        if (message != null && message.getText() != null) {
            return message.getText();
        }
        return "";
    }

    /**
     * Extract contact name from webhook response
     */
    private String getContactName(WhatsAppWebhookResponse.Contact contact) {
        if (contact != null && contact.getName() != null && !contact.getName().trim().isEmpty()) {
            return contact.getName().trim();
        }
        return "Driver"; // Default fallback name
    }

    /**
     * Extract message type from webhook response
     */
    private String getMessageType(WhatsAppWebhookResponse.MessageContent message) {
        if (message != null && message.getType() != null && !message.getType().trim().isEmpty()) {
            return message.getType().toLowerCase(); // Convert to lowercase for API consistency
        }
        return "text"; // Default fallback type
    }

    /**
     * Extract platform from integration type
     */
    private String getPlatform(String integrationType) {
        if (integrationType != null && !integrationType.trim().isEmpty()) {
            // Convert to proper case for API
            return integrationType.toLowerCase().substring(0, 1).toUpperCase() + 
                   integrationType.toLowerCase().substring(1);
        }
        return "Whatsapp"; // Default fallback platform
    }

    /**
     * Parse receivedAt timestamp from webhook response
     * Format: 2025-01-01T10:10:00.000+0000
     */
    private long parseReceivedAtTimestamp(String receivedAt) {
        if (receivedAt == null || receivedAt.trim().isEmpty()) {
            log.warn("No receivedAt timestamp provided, using current time");
            return System.currentTimeMillis();
        }
        
        try {
            // Normalize timezone format for Java parsing
            String normalizedTimestamp = normalizeTimestampFormat(receivedAt);
            
            // Parse ISO 8601 timestamp to milliseconds
            Instant instant = Instant.parse(normalizedTimestamp);
            long timestamp = instant.toEpochMilli();
            log.debug("Parsed timestamp '{}' (normalized: '{}') to {}", receivedAt, normalizedTimestamp, timestamp);
            return timestamp;
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse receivedAt timestamp '{}': {}. Using current time.", receivedAt, e.getMessage());
            return System.currentTimeMillis();
        }
    }

    /**
     * Normalize timestamp format to be compatible with Java's Instant.parse()
     * Converts: 2025-01-01T10:10:00.000+0000 → 2025-01-01T10:10:00.000Z
     * Converts: 2025-01-01T10:10:00.000+0500 → 2025-01-01T10:10:00.000+05:00
     */
    private String normalizeTimestampFormat(String timestamp) {
        if (timestamp == null) {
            return timestamp;
        }
        
        // Handle UTC timezone: +0000 or -0000 → Z
        if (timestamp.endsWith("+0000") || timestamp.endsWith("-0000")) {
            return timestamp.substring(0, timestamp.length() - 5) + "Z";
        }
        
        // Handle other timezones: +0500 → +05:00, -0300 → -03:00
        if (timestamp.matches(".*[+-]\\d{4}$")) {
            String base = timestamp.substring(0, timestamp.length() - 5);
            String sign = timestamp.substring(timestamp.length() - 5, timestamp.length() - 4);
            String hours = timestamp.substring(timestamp.length() - 4, timestamp.length() - 2);
            String minutes = timestamp.substring(timestamp.length() - 2);
            return base + sign + hours + ":" + minutes;
        }
        
        // Return as-is if already in correct format
        return timestamp;
    }

    /**
     * Cleanup expired conversation for a driver if timeout exceeded
     */
    private void cleanupExpiredConversation(String driverPhone) {
        Long lastTimestamp = lastMessageTimestamps.get(driverPhone);
        if (lastTimestamp != null) {
            long timeSinceLastMessage = System.currentTimeMillis() - lastTimestamp;
            if (timeSinceLastMessage > CONVERSATION_TIMEOUT_MS) {
                log.info("🧹 Conversation expired for driver {} (inactive for {} minutes). Resetting conversation.", 
                        driverPhone, timeSinceLastMessage / (60 * 1000));
                
                // Reset chat module conversation
                chatModuleService.resetConversation(driverPhone);
                
                // Remove timestamp tracking
                lastMessageTimestamps.remove(driverPhone);
                
                log.debug("✅ Expired conversation cleaned up for driver: {}", driverPhone);
            } else {
                log.debug("⏰ Conversation for driver {} still active (last activity {} minutes ago)", 
                        driverPhone, timeSinceLastMessage / (60 * 1000));
            }
        } else {
            log.debug("🆕 New conversation starting for driver: {}", driverPhone);
        }
    }

    /**
     * Reset conversation for a driver (for testing or when conversation should restart)
     */
    public void resetConversationState(String driverPhone) {
        log.info("🔄 Resetting conversation for driver: {}", driverPhone);
        
        // Reset chat module conversation
        chatModuleService.resetConversation(driverPhone);
        
        // Clear timestamp tracking
        lastMessageTimestamps.remove(driverPhone);
        
        log.info("✅ Conversation reset completed for driver: {}", driverPhone);
    }

    /**
     * Get current chat module conversation ID for debugging
     */
    public String getCurrentChatConversationId(String driverPhone) {
        return chatModuleService.getCurrentConversationId(driverPhone);
    }

    /**
     * Health check method to verify chat module integration
     */
    public boolean isChatModuleHealthy() {
        try {
            // Test with a simple message to check if chat module is responding
            String testResponse = chatModuleService.sendMessageToChatModule(
                "EG_TRU_a5d34a6f", // Default tenant ID for health check
                "1123", // Default assistant ID for health check
                "health_check_" + System.currentTimeMillis(), // Conversation ID
                "test_health_check", // Driver phone
                "TestDriver", // Driver name
                "Hello", // Message content
                "health_check_" + System.currentTimeMillis(), // Message ID
                System.currentTimeMillis(), // Timestamp
                "text", // Message type
                "Whatsapp" // Platform
            );
            return testResponse != null && !testResponse.contains("having trouble");
        } catch (Exception e) {
            log.error("Chat module health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get conversation status for a driver including timeout info
     */
    public ConversationStatus getConversationStatus(String driverPhone) {
        Long lastTimestamp = lastMessageTimestamps.get(driverPhone);
        String conversationId = chatModuleService.getCurrentConversationId(driverPhone);
        
        if (lastTimestamp == null) {
            return new ConversationStatus(driverPhone, false, null, 0, false, conversationId);
        }
        
        long timeSinceLastMessage = System.currentTimeMillis() - lastTimestamp;
        boolean isActive = timeSinceLastMessage <= CONVERSATION_TIMEOUT_MS;
        long minutesSinceLastMessage = timeSinceLastMessage / (60 * 1000);
        
        return new ConversationStatus(driverPhone, true, lastTimestamp, minutesSinceLastMessage, isActive, conversationId);
    }

    /**
     * Get current timeout configuration in minutes
     */
    public long getConversationTimeoutMinutes() {
        return CONVERSATION_TIMEOUT_MS / (60 * 1000);
    }

    /**
     * Force cleanup of all expired conversations (useful for maintenance)
     */
    public int cleanupAllExpiredConversations() {
        log.info("🧹 Starting cleanup of all expired conversations...");
        int cleanedUp = 0;
        
        // Create a copy of the keys to avoid concurrent modification
        for (String driverPhone : lastMessageTimestamps.keySet().toArray(new String[0])) {
            Long lastTimestamp = lastMessageTimestamps.get(driverPhone);
            if (lastTimestamp != null) {
                long timeSinceLastMessage = System.currentTimeMillis() - lastTimestamp;
                if (timeSinceLastMessage > CONVERSATION_TIMEOUT_MS) {
                    log.debug("Cleaning up expired conversation for driver: {}", driverPhone);
                    chatModuleService.resetConversation(driverPhone);
                    lastMessageTimestamps.remove(driverPhone);
                    cleanedUp++;
                }
            }
        }
        
        log.info("✅ Cleanup completed. {} expired conversations removed.", cleanedUp);
        return cleanedUp;
    }

    /**
     * Conversation status information class
     */
    public static class ConversationStatus {
        private String driverPhone;
        private boolean hasConversation;
        private Long lastMessageTimestamp;
        private long minutesSinceLastMessage;
        private boolean isActive;
        private String conversationId;

        public ConversationStatus(String driverPhone, boolean hasConversation, Long lastMessageTimestamp, 
                                long minutesSinceLastMessage, boolean isActive, String conversationId) {
            this.driverPhone = driverPhone;
            this.hasConversation = hasConversation;
            this.lastMessageTimestamp = lastMessageTimestamp;
            this.minutesSinceLastMessage = minutesSinceLastMessage;
            this.isActive = isActive;
            this.conversationId = conversationId;
        }

        // Getters
        public String getDriverPhone() { return driverPhone; }
        public boolean isHasConversation() { return hasConversation; }
        public Long getLastMessageTimestamp() { return lastMessageTimestamp; }
        public long getMinutesSinceLastMessage() { return minutesSinceLastMessage; }
        public boolean isActive() { return isActive; }
        public String getConversationId() { return conversationId; }
    }
} 