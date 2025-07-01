package com.driver.whatsapp.wrapper.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChatModuleService {

    @Autowired
    private RestTemplate restTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Fixed configuration values
    private static final String CHAT_API_URL = "https://evainternal.bngrenew.com/chat_module/chat";
    private static final String TENANT_ID = "EG_TRU_a5d34a6f";
    private static final String ASSISTANT_ID = "1123";
    private static final String AUTH_TOKEN_SUFFIX = "_5";
    private static final String LANGUAGE_ID = "en-US";
    private static final String LANGUAGE_NAME = "English";

    // Store conversation IDs per driver phone number
    private final Map<String, String> conversationIds = new ConcurrentHashMap<>();

    /**
     * Send message to chat module and get AI response
     */
    public String sendMessageToChatModule(String driverPhone, String driverName, String messageContent, String messageId, long timestamp, String messageType, String platform) {
        try {
            // Generate or get existing conversation ID
            String conversationId = getOrCreateConversationId(driverPhone);
            
            // Create auth token with phone number + suffix
            String authToken = driverPhone.replaceAll("[^0-9]", "") + AUTH_TOKEN_SUFFIX;
            
            // Build request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversation_id", conversationId);
            payload.put("language_id", LANGUAGE_ID);
            payload.put("language_name", LANGUAGE_NAME);
            payload.put("tenant_id", TENANT_ID);
            payload.put("user_id", driverPhone); // Using phone as user_id as mentioned
            payload.put("name", driverName); // Using actual driver name from webhook
            payload.put("phone_no", driverPhone);
            payload.put("auth_token", authToken);
            payload.put("assistant_id", ASSISTANT_ID);
            payload.put("platform", platform); // Using dynamic platform from webhook
            payload.put("text", messageContent);
            payload.put("message_id", messageId);
            payload.put("timestamp", timestamp);
            payload.put("message_type", messageType); // Using dynamic message type from webhook

            log.info("Sending message to chat module for driver {}: {}", driverPhone, messageContent);
            log.debug("Chat module payload: {}", objectMapper.writeValueAsString(payload));

            // Setup HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Make API call
            ResponseEntity<ChatModuleResponse> response = restTemplate.exchange(
                CHAT_API_URL,
                HttpMethod.POST,
                request,
                ChatModuleResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ChatModuleResponse chatResponse = response.getBody();
                log.info("Received chat module response for driver {}: {}", driverPhone, chatResponse.getChunk());
                return chatResponse.getChunk();
            } else {
                log.error("Failed to get valid response from chat module. Status: {}", response.getStatusCode());
                return "I'm having trouble processing your message right now. Please try again.";
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Chat module API timeout for driver {}: {}", driverPhone, e.getMessage());
            return "I'm taking longer than expected to respond. Please try again in a moment.";
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Chat module client error for driver {}: {} - {}", driverPhone, e.getStatusCode(), e.getResponseBodyAsString());
            return "I encountered an error processing your request. Please try again.";
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("Chat module server error for driver {}: {} - {}", driverPhone, e.getStatusCode(), e.getResponseBodyAsString());
            return "The service is temporarily unavailable. Please try again in a few minutes.";
        } catch (Exception e) {
            log.error("Unexpected error calling chat module for driver {}: {}", driverPhone, e.getMessage(), e);
            return "Sorry, I'm having trouble responding right now. Please try again later.";
        }
    }

    /**
     * Get or create conversation ID for a driver
     */
    private String getOrCreateConversationId(String driverPhone) {
        return conversationIds.computeIfAbsent(driverPhone, k -> {
            String newConversationId = UUID.randomUUID().toString();
            log.info("Generated new conversation ID {} for driver {}", newConversationId, driverPhone);
            return newConversationId;
        });
    }

    /**
     * Reset conversation for a driver (useful for testing or when conversation should restart)
     */
    public void resetConversation(String driverPhone) {
        conversationIds.remove(driverPhone);
        log.info("Reset conversation for driver {}", driverPhone);
    }

    /**
     * Get current conversation ID for a driver (for debugging)
     */
    public String getCurrentConversationId(String driverPhone) {
        return conversationIds.get(driverPhone);
    }

    /**
     * Chat Module API Response model
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatModuleResponse {
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("conversation_id")
        private String conversationId;
        
        @JsonProperty("chunk")
        private String chunk;
        
        @JsonProperty("message_id")
        private String messageId;
        
        @JsonProperty("timestamp")
        private Long timestamp;
        
        @JsonProperty("is_end")
        private Boolean isEnd;
        
        @JsonProperty("language_id")
        private String languageId;
        
        @JsonProperty("language_name")
        private String languageName;
        
        @JsonProperty("message_type")
        private String messageType;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        
        public String getChunk() { return chunk; }
        public void setChunk(String chunk) { this.chunk = chunk; }
        
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public Boolean getIsEnd() { return isEnd; }
        public void setIsEnd(Boolean isEnd) { this.isEnd = isEnd; }
        
        public String getLanguageId() { return languageId; }
        public void setLanguageId(String languageId) { this.languageId = languageId; }
        
        public String getLanguageName() { return languageName; }
        public void setLanguageName(String languageName) { this.languageName = languageName; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
    }
} 