package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.model.DriverDetails;
import com.driver.whatsapp.wrapper.model.WhatsAppMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * Example WhatsApp Service showing how to use static ConfigurationCacheService
 * This demonstrates replacing hardcoded values with dynamic configuration
 */
@Service
@Slf4j
public class WhatsAppServiceWithConfig {

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

    public WhatsAppServiceWithConfig() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send initial templated message with configuration from cache
     */
    public void sendInitialDriverMessage(DriverDetails driverDetails) {
        try {
            log.info("Sending initial template message to driver: {} for order: {}", 
                    driverDetails.getDriverPhone(), driverDetails.getOrderId());
            
            // Use configuration from static cache
            String templateName = ConfigurationCacheService.getConfigValue("template_name", "11_start_time_due");
            String callbackData = ConfigurationCacheService.getConfigValue("initial_callback_data", "initial_eta_check");
            
            WhatsAppMessage message = buildTemplatedMessage(driverDetails, templateName, callbackData);
            sendMessage(message, "/whatsapp/1/message/template");
            
            log.info("Initial template message sent successfully using template: {}", templateName);
                    
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send initial template message: {}", e.getMessage(), e);
        }
    }

    /**
     * Send text message using configurable messages
     */
    public void sendTextMessage(String driverPhone, String textMessage, String orderId) {
        try {
            // Get timeout and retry settings from configuration
            int apiTimeout = Integer.parseInt(ConfigurationCacheService.getConfigValue("api_timeout", "30000"));
            int maxRetries = Integer.parseInt(ConfigurationCacheService.getConfigValue("max_retries", "3"));
            
            log.info("Sending message with timeout: {}ms, max retries: {}", apiTimeout, maxRetries);
            
            WhatsAppMessage.SimpleTextMessage message = buildSimpleTextMessage(driverPhone, textMessage, orderId);
            sendSimpleMessage(message, "/whatsapp/1/message/text");
            
        } catch (Exception e) {
            log.error("Error sending text message: {}", e.getMessage(), e);
        }
    }

    /**
     * Send follow-up message for new ETA using configured message template
     */
    public void sendNewEtaRequest(String driverPhone, String orderId) {
        // Get message template from configuration cache
        String messageTemplate = ConfigurationCacheService.getConfigValue(
            "new_eta_message", 
            "We understand you're facing a delay. Please provide your new estimated arrival time (e.g., 10:30 AM)."
        );
        
        sendTextMessage(driverPhone, messageTemplate, orderId);
    }

    /**
     * Send breakdown inquiry using configured options
     */
    public void sendBreakdownInquiry(String driverPhone, String orderId) {
        // Get breakdown inquiry template from configuration
        String inquiryTemplate = ConfigurationCacheService.getConfigValue(
            "breakdown_inquiry_template",
            "We understand you're facing an issue. Please let us know:\n1. Vehicle breakdown\n2. Traffic delay\n3. Other (please specify)"
        );
        
        sendTextMessage(driverPhone, inquiryTemplate, orderId);
    }

    /**
     * Send confirmation with configurable prefix
     */
    public void sendConfirmationMessage(String driverPhone, String confirmationText, String orderId) {
        // Add configurable prefix to confirmation messages
        String confirmationPrefix = ConfigurationCacheService.getConfigValue("confirmation_prefix", "✅ ");
        String fullMessage = confirmationPrefix + confirmationText;
        
        sendTextMessage(driverPhone, fullMessage, orderId);
    }

    /**
     * Build templated message with configurable template name
     */
    private WhatsAppMessage buildTemplatedMessage(DriverDetails driverDetails, String templateName, String callbackData) {
        
        // Get button text from configuration
        String yesButtonText = ConfigurationCacheService.getConfigValue("yes_button_text", "Yes I am on time");
        String noButtonText = ConfigurationCacheService.getConfigValue("no_button_text", "I am late");
        
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

        // Get language from configuration
        String language = ConfigurationCacheService.getConfigValue("template_language", "en");

        WhatsAppMessage.Content content = WhatsAppMessage.Content.builder()
                .templateName(templateName)  // Using configurable template name
                .templateData(templateData)
                .language(language)  // Using configurable language
                .build();

        WhatsAppMessage.Message messageItem = WhatsAppMessage.Message.builder()
                .from(whatsappFromNumber)
                .to(driverDetails.getDriverPhone())
                .messageId(driverDetails.getOrderId())
                .content(content)
                .callbackData(callbackData)  // Using configurable callback data
                .build();

        return WhatsAppMessage.builder()
                .messages(Arrays.asList(messageItem))
                .build();
    }

    /**
     * Build simple text message 
     */
    private WhatsAppMessage.SimpleTextMessage buildSimpleTextMessage(String driverPhone, String textMessage, String orderId) {
        
        // Get default callback data from configuration
        String defaultCallbackData = ConfigurationCacheService.getConfigValue("default_callback_data", "text_message");
        
        WhatsAppMessage.SimpleContent content = WhatsAppMessage.SimpleContent.builder()
                .text(textMessage)
                .build();

        return WhatsAppMessage.SimpleTextMessage.builder()
                .from(whatsappFromNumber)
                .to(driverPhone)
                .messageId(orderId)
                .content(content)
                .callbackData(defaultCallbackData)  // Using configurable callback
                .build();
    }

    /**
     * Send simple message with retry logic based on configuration
     */
    private void sendSimpleMessage(WhatsAppMessage.SimpleTextMessage message, String endpoint) {
        // Get retry configuration
        int maxRetries = Integer.parseInt(ConfigurationCacheService.getConfigValue("max_retries", "3"));
        int retryDelay = Integer.parseInt(ConfigurationCacheService.getConfigValue("retry_delay_ms", "1000"));
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String url = infobipApiUrl + endpoint;
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "App " + infobipApiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                String jsonBody = objectMapper.writeValueAsString(message);
                HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Message sent successfully to {} on attempt {}/{}", message.getTo(), attempt, maxRetries);
                    return; // Success - exit retry loop
                } else {
                    log.warn("WhatsApp API returned error on attempt {}/{}: Status: {}", 
                            attempt, maxRetries, response.getStatusCode());
                }
                
            } catch (Exception e) {
                log.warn("Error on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
            }
            
            // Sleep before retry (except on last attempt)
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // All retries failed
        log.error("CRITICAL: All {} attempts failed for driver {}", maxRetries, message.getTo());
        handleWhatsAppFailure(message, "All retries exhausted");
    }

    // ... rest of the methods remain the same
    private void sendMessage(WhatsAppMessage message, String endpoint) {
        // Implementation similar to sendSimpleMessage with retries from config
    }
    
    private void handleWhatsAppFailure(WhatsAppMessage.SimpleTextMessage message, String errorReason) {
        // Get failure handling configuration
        boolean enableFailureAlerts = Boolean.parseBoolean(
            ConfigurationCacheService.getConfigValue("enable_failure_alerts", "true")
        );
        
        if (enableFailureAlerts) {
            log.error("CRITICAL ALERT: Driver {} did not receive message: {}", 
                     message.getTo(), errorReason);
            // Could trigger notification system here
        }
    }
} 