package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.constants.ConfigurationConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.driver.whatsapp.wrapper.exception.ApiServiceException;
import com.driver.whatsapp.wrapper.service.ConfigurationCacheService;

@Service
@Slf4j
public class ChatModuleService {

    private static final String DEFAULT_CHAT_API_URL = "https://eva-integration.bngrenew.com/chat_module/chat";

    @Autowired
    private RestTemplate restTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String AUTH_TOKEN_SUFFIX = "_5";

    private String getChatApiUrl() {
        return ConfigurationCacheService.getConfigValue(
            ConfigurationConstants.CHAT_MODULE_API_URL,
            DEFAULT_CHAT_API_URL
        );
    }

    /**
     * Send message to chat module with custom tenant and assistant IDs
     */
    public String sendMessageToChatModuleWithConfig(String conversationId,String driverPhone, String driverName, String messageContent, String messageId, long timestamp, String messageType, String platform, String tenantId, String assistantId, String lang) {
        try {
            // Create auth token with phone number + suffix
            messageType = "text";
            String language_name = ConfigurationCacheService.getLanguageName(lang, "English");
            String authToken = driverPhone.replaceAll("[^0-9]", "") + AUTH_TOKEN_SUFFIX;
            
            // Build request payload with custom tenant and assistant IDs
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversation_id", conversationId);
            payload.put("language_id", lang);
            payload.put("language_name", language_name);

            payload.put("tenant_id", tenantId); // Use custom tenant ID
            payload.put("user_id", driverPhone); // Using phone as user_id as mentioned
            payload.put("name", driverName); // Using actual driver name from webhook
            payload.put("phone_no", driverPhone);
            payload.put("auth_token", authToken);
            payload.put("assistant_id", assistantId); // Use custom assistant ID
            payload.put("platform", platform); 
            payload.put("text", messageContent);
            payload.put("message_id", messageId);
            payload.put("timestamp", timestamp);
            payload.put("message_type", messageType); // Using dynamic message type from webhook

            log.info("Sending message to chat module for driver {} with tenant: {}, assistant: {}: {}", driverPhone, tenantId, assistantId, messageContent);
            log.debug("Chat module payload: {}", objectMapper.writeValueAsString(payload));

            // Setup HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            // Make API call
            ResponseEntity<ChatModuleResponse> response = restTemplate.exchange(
                getChatApiUrl(),
                HttpMethod.POST,
                request,
                ChatModuleResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ChatModuleResponse chatResponse = response.getBody();
                log.info("Received chat module response for driver {}: {}", driverPhone, chatResponse.getChunk());
                return chatResponse.getChunk();
            } else {
                log.error("Failed to get valid response from chat module - Status: {}", response.getStatusCode());
                return getDefaultResponse();
            }
        } catch (ResourceAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof ConnectException) {
                log.error("Connection timeout while calling chat module for driver: {} - Chat module might be down or unreachable", driverPhone, cause);
            } else if (cause instanceof SocketTimeoutException) {
                log.error("Read timeout while calling chat module for driver: {} - Chat module took too long to respond", driverPhone, cause);
            } else {
                log.error("Network error while calling chat module for driver: {} - {}", driverPhone, e.getMessage(), e);
            }
            return getDefaultResponse();
            
        } catch (Exception e) {
            // Any other unexpected error
            log.error("Error calling chat module for driver: {} - {}", driverPhone, e.getMessage(), e);
            return getDefaultResponse();
        }
    }

    public void sendToChatModule(String message, String phoneNumber, String messageType) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", message);
            requestBody.put("phone_number", phoneNumber);
            requestBody.put("message_type", messageType);

            ResponseEntity<String> response = restTemplate.postForEntity(
                getChatApiUrl(),
                requestBody,
                String.class
            );

            log.info("Chat module response status: {}", response.getStatusCode());
            log.debug("Chat module response body: {}", response.getBody());
        } catch (Exception ex) {
            log.error("Error sending message to chat module: {}", ex.getMessage(), ex);
            throw new ApiServiceException("ChatModule", "Failed to send message to chat module", ex);
        }
    }

    private String getDefaultResponse() {
        return "I'm having trouble processing your message right now. Please try again in a moment.";
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