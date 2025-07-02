package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.constants.ConfigurationConstants;
import com.driver.whatsapp.wrapper.model.DriverDetails;
import com.driver.whatsapp.wrapper.model.WhatsAppMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
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

    public WhatsAppService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send initial templated message to driver asking about ETA status
     */
    public void sendInitialDriverMessage(DriverDetails driverDetails) {
        try {
            log.info("Sending initial template message to driver: {} for order: {}", 
                    driverDetails.getDriverPhone(), driverDetails.getOrderId());
            
            // Get API assistant mapping for driver_details WhatsApp
            String tenantId = ConfigurationCacheService.getTenantId("driver_details", "whatsapp");
            String assistantId = ConfigurationCacheService.getAssistantId("driver_details", "whatsapp");
            
            if (tenantId != null && assistantId != null) {
                log.info("Using tenant: {}, assistant: {} for driver_details WhatsApp", tenantId, assistantId);
            }
            
            WhatsAppMessage message = buildTemplatedMessage(driverDetails);
            sendMessage(message, "/whatsapp/1/message/template");
            
            log.info("Initial template message sent successfully to driver: {} for order: {}", 
                    driverDetails.getDriverPhone(), driverDetails.getOrderId());
                    
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send initial template message to driver {} for order {}: {}", 
                     driverDetails.getDriverPhone(), driverDetails.getOrderId(), e.getMessage(), e);
            
            // This is critical - if initial message fails, the whole workflow stops
            log.error("ALERT: Driver {} will not receive any communication for order {}!", 
                     driverDetails.getDriverPhone(), driverDetails.getOrderId());
        }
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
     * Send follow-up message for new ETA
     */
    public void sendNewEtaRequest(String driverPhone, String orderId) {
        // Get configured message template
        String message = ConfigurationCacheService.getConfigValue(
            ConfigurationConstants.NEW_ETA_MESSAGE, 
            "We understand you're facing a delay. Please provide your new estimated arrival time (e.g., 10:30 AM)."
        );
        
        // Get API assistant mapping for ETA update
        String tenantId = ConfigurationCacheService.getTenantId("eta_update", "whatsapp");
        String assistantId = ConfigurationCacheService.getAssistantId("eta_update", "whatsapp");
        
        log.info("Sending ETA request using tenant: {}, assistant: {}", tenantId, assistantId);
        sendTextMessage(driverPhone, message, orderId);
    }

    /**
     * Send breakdown inquiry message
     */
    public void sendBreakdownInquiry(String driverPhone, String orderId) {
        String message = "We understand you're facing an issue. Please let us know:\n1. Vehicle breakdown\n2. Traffic delay\n3. Other (please specify)";
        sendTextMessage(driverPhone, message, orderId);
    }

    /**
     * Send confirmation message
     */
    public void sendConfirmationMessage(String driverPhone, String confirmationText, String orderId) {
        sendTextMessage(driverPhone, confirmationText, orderId);
    }

    /**
     * Build templated message for initial driver contact
     */
    private WhatsAppMessage buildTemplatedMessage(DriverDetails driverDetails) {
        // Get button text from configuration
        String yesButtonText = ConfigurationCacheService.getConfigValue(
            ConfigurationConstants.YES_BUTTON_TEXT, "Yes I am on time");
        String noButtonText = ConfigurationCacheService.getConfigValue(
            ConfigurationConstants.NO_BUTTON_TEXT, "I am late");
            
        WhatsAppMessage.Button yesButton = WhatsAppMessage.Button.builder()
                .type("QUICK_REPLY")
                .parameter(yesButtonText)
                .build();

        WhatsAppMessage.Button noButton = WhatsAppMessage.Button.builder()
                .type("QUICK_REPLY")
                .parameter(noButtonText)
                .build();

        WhatsAppMessage.Body body = WhatsAppMessage.Body.builder()
                .placeholders(Arrays.asList(
                        driverDetails.getDriverName(),
                        driverDetails.getPickup(),
                        driverDetails.getDropoff(),
                        driverDetails.getEtaTime()
                ))
                .build();

        WhatsAppMessage.TemplateData templateData = WhatsAppMessage.TemplateData.builder()
                .body(body)
                .buttons(Arrays.asList(yesButton, noButton))
                .build();

        // Get template configuration
        String templateName = ConfigurationCacheService.getConfigValue(
            ConfigurationConstants.WHATSAPP_TEMPLATE_NAME, "11_start_time_due");
        String templateLanguage = ConfigurationCacheService.getConfigValue(
            ConfigurationConstants.TEMPLATE_LANGUAGE, "en");

        WhatsAppMessage.Content content = WhatsAppMessage.Content.builder()
                .templateName(templateName)
                .templateData(templateData)
                .language(templateLanguage)
                .build();

        // Get callback data configuration
        String callbackData = ConfigurationCacheService.getConfigValue(
            ConfigurationConstants.INITIAL_CALLBACK_DATA, "initial_eta_check");

        WhatsAppMessage.Message messageItem = WhatsAppMessage.Message.builder()
                .from(whatsappFromNumber)
                .to(driverDetails.getDriverPhone())
                .messageId(driverDetails.getOrderId())
                .content(content)
                .callbackData(callbackData)
                // .notifyUrl(webhookUrl + "/whatsapp/callback")
                .build();

                log.info("notifyUrl: {}", webhookUrl + "/whatsapp/callback");

        return WhatsAppMessage.builder()
                .messages(Arrays.asList(messageItem))
                .build();
    }

    /**
     * Build simple text message (direct format, no messages array)
     */
    private WhatsAppMessage.SimpleTextMessage buildSimpleTextMessage(String driverPhone, String textMessage, String orderId) {
        WhatsAppMessage.SimpleContent content = WhatsAppMessage.SimpleContent.builder()
                .text(textMessage)
                .build();

        WhatsAppMessage.SimpleTextMessage message = WhatsAppMessage.SimpleTextMessage.builder()
                .from(whatsappFromNumber)
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
            String url = infobipApiUrl + endpoint;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "App " + infobipApiKey);
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
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Network/connection issues
            log.error("WhatsApp API connection failed for {}: {}", message.getTo(), e.getMessage());
            handleWhatsAppFailure(message, "Connection failed: " + e.getMessage());
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx errors
            log.error("WhatsApp API client error for {}: {} - {}", 
                     message.getTo(), e.getStatusCode(), e.getResponseBodyAsString());
            handleWhatsAppFailure(message, "Client error: " + e.getStatusCode());
            
        } catch (org.springframework.web.client.HttpServerErrorException e) {
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
            String url = infobipApiUrl + endpoint;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "App " + infobipApiKey);
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
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Network/connection issues
            log.error("WhatsApp API connection failed for templated message: {}", e.getMessage());
            handleTemplateMessageFailure(message, "Connection failed: " + e.getMessage());
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx errors
            log.error("WhatsApp API client error for templated message: {} - {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            handleTemplateMessageFailure(message, "Client error: " + e.getStatusCode());
            
        } catch (org.springframework.web.client.HttpServerErrorException e) {
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