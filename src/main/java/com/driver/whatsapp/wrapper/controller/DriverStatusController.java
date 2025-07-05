package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import com.driver.whatsapp.wrapper.service.TruKKerService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import com.driver.whatsapp.wrapper.model.FlowType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/wawrapper/driver-status")
public class DriverStatusController {

    private static final Logger log = LoggerFactory.getLogger(DriverStatusController.class);

    @Autowired
    private TruKKerService truKKerService;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Value("${trukker.api.otr.url}")
    private String trukkerOtrUrl;

    @Value("${trukker.api.loading-confirmation.url}")
    private String trukkerLoadingConfirmationUrl;

    @Value("${trukker.api.generic-connect.url}")
    private String trukkerGenericConnectUrl;

    @Value("${trukker.api.doc-reminder.url}")
    private String trukkerDocReminderUrl;

    @PostMapping("/{action}")
    public ResponseEntity<String> updateDriverStatus(
            @PathVariable String action,
            @RequestBody DriverStatusRequest request
    ) {
        // Check for mandatory conversationId
        if (request.getConversationId() == null || request.getConversationId().isBlank()) {
            return ResponseEntity.badRequest().body("conversationId is required");
        }
        // Fetch TransactionDetail using conversationId
        Optional<TransactionDetail> txOpt = transactionDetailRepository.findByConversationId(request.getConversationId());
        if (txOpt.isEmpty()) {
            return ResponseEntity.status(200).body("TransactionDetail not found for conversationId: " + request.getConversationId());
        }
        TransactionDetail tx = txOpt.get();
        String flowName = tx.getFlowName();
        String url = null;

        if (flowName != null) {
                    try {
                        FlowType flowType = FlowType.fromValue(flowName);
                        switch (flowType) {
                            case OTR:
                                /** CaptureBotResponseForOTR  */
                                url = trukkerOtrUrl;
                                break;

                            case LOADING_CONFIRMATION:
                                /** CaptureBotResponseForLoadingConfirmation */
                                url = trukkerLoadingConfirmationUrl;

                                break;
                            case ORDER_COMPLETION:
                                /** CaptureBotResponseForDocReminder */
                                url = trukkerDocReminderUrl;

                                break;

                            case STATUS_FOLLOW_UP:
                                /** CaptureBotResponseForGenericConnect */
                                url = trukkerGenericConnectUrl;
                                break;

                            default:
                                log.info("Not a valid flow type as if now.");
                                break;
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown flow type: {}. Skipping TruKKerService call.", flowName);
                    }
                }

        switch (action.toLowerCase()) {
            case "on-time":
                truKKerService.updateDriverStatusOnTime(url, tx);
                break;
            case "new-eta":
                truKKerService.updateNewEta(url, tx, request.getDelayReason(), request.getNewEta());
                break;
            case "breakdown":
                truKKerService.updateDriverStatusBreakdown(url, tx, request.getDelayReason());
                break;
            default:
                return ResponseEntity.status(200).body("Invalid action: " + action);
        }
        return ResponseEntity.ok("Driver status updated for action: " + action);
    }

    @Data
    public static class DriverStatusRequest {

        @JsonProperty("conversationId")
        private String conversationId;

        @JsonProperty("delayReason")
        private String delayReason;

        @JsonProperty("newEta")
        private String newEta; // ISO-8601 string
        
        // getters and setters
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public String getDelayReason() { return delayReason; }
        public void setDelayReason(String delayReason) { this.delayReason = delayReason; }
        public String getNewEta() { return newEta; }
        public void setNewEta(String newEta) { this.newEta = newEta; }
    }
} 