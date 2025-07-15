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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.HashMap;
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

            ApiAssistantMappingId mappingKey = new ApiAssistantMappingId();
            mappingKey.setApiName(flowName);
            mappingKey.setCommunicationMode(communicationMode);
            mappingKey.setLang("en"); // Default language
            
            ApiAssistantMapping apiAssistantMapping = apiAssistantMappingRepository.findById(mappingKey)
                .orElseThrow(() -> new RuntimeException("Api Assistant Mapping not found for flow: " + flowName + " and mode: " + communicationMode));

            String tenantId = apiAssistantMapping.getTenantId();
            String assistantId = apiAssistantMapping.getAssistantId();
            
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
     * For text messages, returns the text content
     * For document messages, returns the entire message object as JSON string
     * For media messages (image, audio, video), returns the entire message object as JSON string
     */
    private String getMessageContent(WhatsAppWebhookResponse.MessageContent message) {
        if (message != null) {
            // For text messages
            if (message.getText() != null) {
                return message.getText();
            }
            // For document messages, return the entire message object as JSON
            if ("document".equalsIgnoreCase(message.getType())) {
                try {
                    // Create a JSON representation of the message object
                    Map<String, Object> messageJson = new HashMap<>();
                    messageJson.put("url", message.getUrl());
                    messageJson.put("caption", message.getCaption());
                    messageJson.put("type", message.getType());
                    
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.writeValueAsString(messageJson);
                } catch (Exception e) {
                    log.error("Error converting document message to JSON: {}", e.getMessage());
                    return "Document uploaded";
                }
            }
            // For media messages (image, audio, video), return the entire message object as JSON
            if ("image".equalsIgnoreCase(message.getType()) || 
                "audio".equalsIgnoreCase(message.getType()) || 
                "video".equalsIgnoreCase(message.getType())) {
                try {
                    // Create a JSON representation of the message object
                    Map<String, Object> messageJson = new HashMap<>();
                    messageJson.put("url", message.getUrl());
                    messageJson.put("type", message.getType());
                    
                    // Only include caption if it exists and is not empty
                    if (message.getCaption() != null && !message.getCaption().trim().isEmpty()) {
                        messageJson.put("caption", message.getCaption().trim());
                    }
                    
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.writeValueAsString(messageJson);
                } catch (Exception e) {
                    log.error("Error converting {} message to JSON: {}", message.getType(), e.getMessage());
                    return message.getType().substring(0, 1).toUpperCase() + message.getType().substring(1) + " uploaded";
                }
            }
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





} 