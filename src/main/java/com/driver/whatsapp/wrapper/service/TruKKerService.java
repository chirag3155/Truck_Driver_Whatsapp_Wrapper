package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.model.TruKKerUpdateRequest;
import com.driver.whatsapp.wrapper.model.OtrBotResponseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.IOException;

@Service
@Slf4j
public class TruKKerService {

    private final ObjectMapper objectMapper;

    @Value("${trukker.api.key:RFIqPZDsp0aZKvVW78CgDFWVYh1UEE5Rs0PmSLCW16lUF9rJ2u9rlPb3t4Dx3scA}")
    private String trukkerApiKey;

    @Value("${trukker.api.otr.url}")
    private String trukkerOtrUrl;

    @Value("${trukker.api.loading-confirmation.url}")
    private String trukkerLoadingConfirmationUrl;

    @Value("${trukker.api.generic-connect.url}")
    private String trukkerGenericConnectUrl;

    @Value("${trukker.api.doc-reminder.url}")
    private String trukkerDocReminderUrl;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private Environment environment;

    private final RestTemplate restTemplate = new RestTemplate();

    public TruKKerService() {
        this.objectMapper = new ObjectMapper();
    }

    private String loadTemplate() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("trukker-template.json")) {
            if (is == null) throw new RuntimeException("trukker-template.json not found in resources");
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template", e);
        }
    }

    private String buildRequestBodyFromTemplate(
        TransactionDetail tx,
        String status,
        String errorMessage,
        boolean loadingCompleted,
        String newEta,
        boolean breakdown,
        String currentStatus
    ) {
        String json = loadTemplate();

        String actionType = null;

        if(tx.getCommunicationMode().equalsIgnoreCase("whatsapp")){
            actionType = "WhatsAppBot";
        } else if(tx.getCommunicationMode().equalsIgnoreCase("calling")){
            actionType = "VoiceCallBot";
        } else {
            actionType = "HumanEscalation";
        }

        String type = null;
        if(tx.getFlowName().equalsIgnoreCase("LOADING_CONFIRMATION")){
            type = "LoadingConfirmation";
        } else if(tx.getFlowName().equalsIgnoreCase("ORDER_COMPLETION")){
            type = "DocReminder";
        } else if(tx.getFlowName().equalsIgnoreCase("OTR")){
            type = "OTR";
        } else if(tx.getFlowName().equalsIgnoreCase("STATUS_FOLLOW_UP")){
            type = "GenericConnect";
        }
        
        json = json.replace("${STATUS}", status != null ? status : "");
        json = json.replace("${TRNX_ID}", tx.getTransactionId() != null ? tx.getTransactionId() : "");
        json = json.replace("${ACTION_TYPE}", actionType != null ? actionType : "");
        json = json.replace("${TYPE}", type != null ? type : "");
        json = json.replace("${ERR_MSG}", errorMessage != null ? errorMessage : "");
        json = json.replace("${UNIQUE_ID}", tx.getUniqueId() != null ? tx.getUniqueId() : "");
        json = json.replace("${TRIP_ID}", tx.getTripId() != null ? tx.getTripId() : "");
        json = json.replace("${ORDER_ID}", tx.getOrderNumber() != null ? tx.getOrderNumber() : "");
        json = json.replace("${IS_REACHED}", String.valueOf(false));
        json = json.replace("${LOADING_COMPLETED}", String.valueOf(loadingCompleted));
        json = json.replace("${NEW_ETA}", newEta != null ? newEta.toString() : "");
        json = json.replace("${breakdown}", String.valueOf(breakdown));
        if(currentStatus != null){
            json = json.replace("Upcoming", currentStatus);
        }
        return json;
    }

    /**
     * Update driver status when they confirm they're on time
     */
    public void updateDriverStatusOnTime(String url, TransactionDetail tx,String newEta) {
        try {
           
            String requestBody = buildRequestBodyFromTemplate(
                tx, "Success", "", false, newEta!=null?newEta:null, false,null
            );
            sendToTrukkerAndCloseTransaction(url, tx, requestBody);
            log.info("Updated TruKKer system - Driver on time for order: {}", tx.getOrderNumber());
        } catch (Exception e) {
            log.error("Error updating TruKKer system for on-time status: {}", e.getMessage(), e);
        }
    }

    /**
     * Update new ETA when driver provides revised time
     */
    public void updateNewEta(String url, TransactionDetail tx, String delayReason, String newEta) {
        try {
            String requestBody = buildRequestBodyFromTemplate(
                tx, "success", delayReason, false, newEta!=null?newEta:null, false,null
            );
            sendToTrukkerAndCloseTransaction(url, tx, requestBody);
            log.info("Updated TruKKer system - New ETA {} for order: {}", tx.getNewETA(), tx.getOrderNumber());
        } catch (Exception e) {
            log.error("Error updating TruKKer system for new ETA: {}", e.getMessage(), e);
        }
    }

    /**
     * Update driver status when they report a breakdown
     */
    public void updateDriverStatusBreakdown(String url, TransactionDetail tx, String breakdownReason,String currentStatus) {
        try {
            String requestBody = buildRequestBodyFromTemplate(
                tx, "failed", breakdownReason, false, null, true,currentStatus
            );
            sendToTrukkerAndCloseTransaction(url, tx, requestBody);
            log.info("Updated TruKKer system - Driver breakdown for order: {}", tx.getOrderNumber());
        } catch (Exception e) {
            log.error("Error updating TruKKer system for breakdown status: {}", e.getMessage(), e);
        }
    }

    public void updateTrukkerForHumanEscalation(String url, TransactionDetail tx) {
        try {
            String requestBody = buildRequestBodyFromTemplate(
                tx, "failed", "Call Not Received", false, null, true,""
            );
            sendToTrukkerAndCloseTransaction(url, tx, requestBody);
            log.info("Updated TruKKer system - Human escalation for order: {}", tx.getOrderNumber());
        } catch (Exception e) {
            log.error("Error updating TruKKer system for human escalation: {}", e.getMessage(), e);
        }
    }

    private void sendToTrukkerAndCloseTransaction(String url, TransactionDetail tx, String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", trukkerApiKey);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        try {
            // Print request payload and headers
            log.info("[TruKKer API] Request URL: {}", url);
            log.info("[TruKKer API] Request Headers: {}", headers);
            log.info("[TruKKer API] Request Payload: {}", requestBody);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("TruKKer API Response: {}", response.getBody());
        } catch (Exception e) {
            log.error("Error sending request to TruKKer: {}", e.getMessage(), e);
        } finally {
            tx.setState("close");
            transactionDetailRepository.save(tx);
        }
    }
} 