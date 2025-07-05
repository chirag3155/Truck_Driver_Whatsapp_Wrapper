package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.entity.TransactionDetailId;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.driver.whatsapp.wrapper.model.FlowType;
import com.driver.whatsapp.wrapper.service.TruKKerService;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wawrapper/calling/callback")
@Slf4j
public class CallingCallbackController {

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private TruKKerService truKKerService;

    @Value("${trukker.api.otr.url}")
    private String trukkerOtrUrl;

    @Value("${trukker.api.loading-confirmation.url}")
    private String trukkerLoadingConfirmationUrl;

    @Value("${trukker.api.generic-connect.url}")
    private String trukkerGenericConnectUrl;

    @Value("${trukker.api.doc-reminder.url}")
    private String trukkerDocReminderUrl;

    @Operation(
        summary = "Callback for call status update",
        description = "Receives callback with transactionId, number, status (success/failure), and updatedConversationId. Updates the transaction detail accordingly."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Transaction updated\"}")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Transaction not found or bad request",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{\"status\": \"error\", \"message\": \"Transaction not found\"}")
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{\"status\": \"error\", \"message\": \"Some error message\"}")
            )
        )
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> handleCallback(
        @RequestBody CallbackRequest request) {
        log.info("[CallingCallback] Received callback: {}", request);
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<TransactionDetail> optionalTx = transactionDetailRepository.findByTransactionId(request.getTransactionId());
            if (optionalTx.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Transaction not found");
                return ResponseEntity.badRequest().body(response);
            }
            TransactionDetail tx = optionalTx.get();
            // Update conversation ID
            tx.setConversationId(request.getUpdatedConversationId());
            // Optionally update statusCode if status is success
            if ("success".equalsIgnoreCase(request.getStatus())) {
                tx.setStatusCode("Call_Success");
                transactionDetailRepository.save(tx);
            } else {
                tx.setStatusCode("Call_Failure");
                tx.setState("Close");
                tx.setCommunicationMode("human");

                // Flow-based routing to TruKKerService
                String flowName = tx.getFlowName();

                if (flowName != null) {
                    try {
                        FlowType flowType = FlowType.fromValue(flowName);
                        switch (flowType) {
                            case OTR:
                                /** CaptureBotResponseForOTR  */
                                truKKerService.updateTrukkerForHumanEscalation(trukkerOtrUrl, tx);
                                
                                break;

                            case LOADING_CONFIRMATION:
                                /** CaptureBotResponseForLoadingConfirmation */
                                truKKerService.updateTrukkerForHumanEscalation(trukkerLoadingConfirmationUrl, tx);

                                break;
                            case ORDER_COMPLETION:
                                /** CaptureBotResponseForDocReminder */
                                truKKerService.updateTrukkerForHumanEscalation(trukkerDocReminderUrl, tx);

                                break;

                            case STATUS_FOLLOW_UP:
                                /** CaptureBotResponseForGenericConnect */
                                truKKerService.updateTrukkerForHumanEscalation(trukkerGenericConnectUrl, tx);
                                break;

                            default:
                                log.info("Not a valid flow type as if now.");
                                break;
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown flow type: {}. Skipping TruKKerService call.", flowName);
                    }
                }
            }
            
            response.put("status", "ok");
            response.put("message", "Transaction updated");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[CallingCallback] Error processing callback: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(200).body(response);
        }
    }

            
    @Data
    @NoArgsConstructor
    @ToString
    public static class CallbackRequest {
        private String transactionId;
        private String number;
        private String status;
        private String updatedConversationId;
    }
} 