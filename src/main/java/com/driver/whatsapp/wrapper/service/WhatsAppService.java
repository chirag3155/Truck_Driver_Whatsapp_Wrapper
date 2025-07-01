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
            WhatsAppMessage message = buildTemplatedMessage(driverDetails);
            sendMessage(message, "/whatsapp/1/message/template");
            log.info("Initial message sent to driver: {}", driverDetails.getDriverPhone());
        } catch (Exception e) {
            log.error("Error sending initial message to driver: {}", e.getMessage(), e);
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
        String message = "We understand you're facing a delay. Please provide your new estimated arrival time (e.g., 10:30 AM).";
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
        WhatsAppMessage.Button yesButton = WhatsAppMessage.Button.builder()
                .type("QUICK_REPLY")
                .parameter("Yes I am on time")
                .build();

        WhatsAppMessage.Button noButton = WhatsAppMessage.Button.builder()
                .type("QUICK_REPLY")
                .parameter("I am late")
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

        WhatsAppMessage.Content content = WhatsAppMessage.Content.builder()
                .templateName("11_start_time_due")
                .templateData(templateData)
                .language("en")
                .build();

        WhatsAppMessage.Message messageItem = WhatsAppMessage.Message.builder()
                .from(whatsappFromNumber)
                .to(driverDetails.getDriverPhone())
                .messageId(driverDetails.getOrderId())
                .content(content)
                .callbackData("initial_eta_check")
                .notifyUrl(webhookUrl + "/whatsapp/callback")
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
                .notifyUrl(webhookUrl + "/whatsapp/callback")
                .build();
                
        log.info("notifyUrl: {}", webhookUrl + "/whatsapp/callback");
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
                log.info("Message sent successfully: {}", response.getBody());
            } else {
                log.error("Error sending message. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception while sending message: {}", e.getMessage(), e);
        }
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
                log.info("Message sent successfully: {}", response.getBody());
            } else {
                log.error("Error sending message. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception while sending message: {}", e.getMessage(), e);
        }
    }
} 