package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.model.WhatsAppMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;

import java.util.Arrays;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class WhatsAppService {

    @Value("${infobip.api.url}")
    private String infobipApiUrl;

    @Value("${infobip.api.key}")
    private String infobipApiKey;

    @Value("${infobip.whatsapp.from}")
    private String whatsappFromNumber;

    @Value("${app.webhook.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public WhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send text message to driver
     */
    public void sendTextMessage(String driverPhone, String textMessage, String orderId) {
        try {
            WhatsAppMessage.SimpleTextMessage message = buildSimpleTextMessage(driverPhone, textMessage, orderId);
            sendSimpleMessage(message, "/whatsapp/1/message/text");
            log.info("Text message sent to driver: {}", driverPhone);
        } catch (Exception e) {
            log.error("Error sending text message to driver: {}", e.getMessage(), e);
        }
    }



    /**
     * Build simple text message (direct format, no messages array)
     */
    private WhatsAppMessage.SimpleTextMessage buildSimpleTextMessage(String driverPhone, String textMessage, String orderId) {
        WhatsAppMessage.SimpleContent content = WhatsAppMessage.SimpleContent.builder()
                .text(textMessage)
                .build();

        WhatsAppMessage.SimpleTextMessage message = WhatsAppMessage.SimpleTextMessage.builder()
                .from(whatsappFromNumber) // This line was removed
                .to(driverPhone)
                .messageId(orderId)
                .content(content)
                .callbackData("text_message")
                // .notifyUrl(webhookUrl + "/whatsapp/callback")
                .build();
                
        // log.info("notifyUrl: {}", webhookUrl + "/whatsapp/callback");
        return message;
    }

    /**
     * Send simple message to Infobip API (for text messages)
     */
    private void sendSimpleMessage(WhatsAppMessage.SimpleTextMessage message, String endpoint) {
        try {
            String url = infobipApiUrl + endpoint; // This line was removed
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "App " + infobipApiKey); // This line was removed
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(message);
            log.info("Sending simple message JSON: {}", jsonBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Message sent successfully to {}: {}", message.getTo(), response.getBody());
            } else {
                log.error("WhatsApp API returned error for {}: Status: {}, Body: {}", 
                         message.getTo(), response.getStatusCode(), response.getBody());
                handleWhatsAppFailure(message, "API Error: " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof ConnectException) {
                log.error("Connection timeout while sending WhatsApp message to: {}. Infobip API might be down or unreachable. Error: {}", 
                    message.getTo(), cause.getMessage());
            } else if (cause instanceof SocketTimeoutException) {
                log.error("Read timeout while sending WhatsApp message to: {}. Infobip API took too long to respond. Error: {}", 
                    message.getTo(), cause.getMessage());
            } else {
                log.error("Network error while sending WhatsApp message to: {}. Error: {}", 
                    message.getTo(), e.getMessage());
            }
            handleWhatsAppFailure(message, "Connection failed: " + e.getMessage());
            
        } catch (HttpClientErrorException e) {
            // 4xx errors
            log.error("WhatsApp API client error for {}: {} - {}", 
                     message.getTo(), e.getStatusCode(), e.getResponseBodyAsString());
            handleWhatsAppFailure(message, "Client error: " + e.getStatusCode());
            
        } catch (HttpServerErrorException e) {
            // 5xx errors  
            log.error("WhatsApp API server error for {}: {} - {}", 
                     message.getTo(), e.getStatusCode(), e.getResponseBodyAsString());
            handleWhatsAppFailure(message, "Server error: " + e.getStatusCode());
            
        } catch (Exception e) {
            log.error("Unexpected error sending WhatsApp message to {}: {}", message.getTo(), e.getMessage(), e);
            handleWhatsAppFailure(message, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Handle WhatsApp API failures - implement fallback mechanisms
     */
    private void handleWhatsAppFailure(WhatsAppMessage.SimpleTextMessage message, String errorReason) {
        log.warn("WhatsApp message failed for driver {}: {} - Message: {}", 
                message.getTo(), errorReason, message.getContent().getText());
        
        // TODO: Implement fallback mechanisms:
        // 1. Store message for retry later
        // 2. Send SMS as backup
        // 3. Send email notification
        // 4. Alert operations team
        
        // For now, just log the failure
        log.error("CRITICAL: Driver {} did not receive message: '{}'", 
                 message.getTo(), message.getContent().getText());
    }

    /**
     * Send message to Infobip API
     */
    private void sendMessage(WhatsAppMessage message, String endpoint) {
        try {
            String url = infobipApiUrl + endpoint; // This line was removed
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "App " + infobipApiKey); // This line was removed
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(message);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Templated message sent successfully: {}", response.getBody());
            } else {
                log.error("WhatsApp API returned error for templated message: Status: {}, Body: {}", 
                         response.getStatusCode(), response.getBody());
                handleTemplateMessageFailure(message, "API Error: " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof ConnectException) {
                log.error("Connection timeout while sending WhatsApp message to: {}. Infobip API might be down or unreachable. Error: {}", 
                    message.getMessages().get(0).getTo(), cause.getMessage());
            } else if (cause instanceof SocketTimeoutException) {
                log.error("Read timeout while sending WhatsApp message to: {}. Infobip API took too long to respond. Error: {}", 
                    message.getMessages().get(0).getTo(), cause.getMessage());
            } else {
                log.error("Network error while sending WhatsApp message to: {}. Error: {}", 
                    message.getMessages().get(0).getTo(), e.getMessage());
            }
            handleTemplateMessageFailure(message, "Connection failed: " + e.getMessage());
            
        } catch (HttpClientErrorException e) {
            // 4xx errors
            log.error("WhatsApp API client error for templated message: {} - {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            handleTemplateMessageFailure(message, "Client error: " + e.getStatusCode());
            
        } catch (HttpServerErrorException e) {
            // 5xx errors  
            log.error("WhatsApp API server error for templated message: {} - {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            handleTemplateMessageFailure(message, "Server error: " + e.getStatusCode());
            
        } catch (Exception e) {
            log.error("Unexpected error sending templated WhatsApp message: {}", e.getMessage(), e);
            handleTemplateMessageFailure(message, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Handle template message failures - critical since this starts the conversation
     */
    private void handleTemplateMessageFailure(WhatsAppMessage message, String errorReason) {
        // Extract driver phone from the first message in the array
        String driverPhone = message.getMessages().get(0).getTo();
        String orderId = message.getMessages().get(0).getMessageId();
        
        log.error("CRITICAL: Initial template message failed for driver {}: {} - Order: {}", 
                 driverPhone, errorReason, orderId);
        
        // TODO: Implement fallback mechanisms for initial message:
        // 1. Store failed initial message for retry
        // 2. Send simple text message as fallback instead of template
        // 3. Alert operations team immediately (this is critical!)
        // 4. Send SMS as backup
        
        // For now, just log the critical failure
        log.error("ALERT: Driver {} never received initial message for order {}. Conversation will not start!", 
                 driverPhone, orderId);
    }
} 