package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.model.TruKKerUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class TruKKerService {

    private final ObjectMapper objectMapper;

    public TruKKerService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Update driver status when they confirm they're on time
     */
    public void updateDriverStatusOnTime(String orderId, String driverPhone) {
        try {
            TruKKerUpdateRequest request = TruKKerUpdateRequest.builder()
                    .orderId(orderId)
                    .status("on_time")
                    .driverPhone(driverPhone)
                    .timestamp(getCurrentTimestamp())
                    .build();

            sendUpdateToTruKKer(request, "/api/shipments/updateStatus");
            log.info("Updated TruKKer system - Driver on time for order: {}", orderId);
        } catch (Exception e) {
            log.error("Error updating TruKKer system for on-time status: {}", e.getMessage(), e);
        }
    }

    /**
     * Update driver status when they report a delay
     */
    public void updateDriverStatusDelayed(String orderId, String driverPhone, String reason) {
        try {
            TruKKerUpdateRequest request = TruKKerUpdateRequest.builder()
                    .orderId(orderId)
                    .status("delayed")
                    .driverPhone(driverPhone)
                    .reason(reason)
                    .timestamp(getCurrentTimestamp())
                    .build();

            sendUpdateToTruKKer(request, "/api/shipments/updateStatus");
            log.info("Updated TruKKer system - Driver delayed for order: {}", orderId);
        } catch (Exception e) {
            log.error("Error updating TruKKer system for delayed status: {}", e.getMessage(), e);
        }
    }

    /**
     * Update new ETA when driver provides revised time
     */
    public void updateNewEta(String orderId, String newEta, String driverPhone) {
        try {
            TruKKerUpdateRequest request = TruKKerUpdateRequest.builder()
                    .orderId(orderId)
                    .status("delayed")
                    .newEta(newEta)
                    .driverPhone(driverPhone)
                    .timestamp(getCurrentTimestamp())
                    .build();

            sendUpdateToTruKKer(request, "/api/shipments/updateEta");
            log.info("Updated TruKKer system - New ETA {} for order: {}", newEta, orderId);
        } catch (Exception e) {
            log.error("Error updating TruKKer system for new ETA: {}", e.getMessage(), e);
        }
    }

    /**
     * Update driver status when they report a breakdown
     */
    public void updateDriverStatusBreakdown(String orderId, String driverPhone, String breakdownReason) {
        try {
            TruKKerUpdateRequest request = TruKKerUpdateRequest.builder()
                    .orderId(orderId)
                    .status("breakdown")
                    .driverPhone(driverPhone)
                    .reason(breakdownReason)
                    .timestamp(getCurrentTimestamp())
                    .build();

            sendUpdateToTruKKer(request, "/api/shipments/updateStatus");
            log.info("Updated TruKKer system - Driver breakdown for order: {}", orderId);
        } catch (Exception e) {
            log.error("Error updating TruKKer system for breakdown status: {}", e.getMessage(), e);
        }
    }

    /**
     * Send general status update to TruKKer
     */
    public void updateDriverStatus(String orderId, String status, String newEta, String driverPhone, String reason) {
        try {
            TruKKerUpdateRequest.TruKKerUpdateRequestBuilder requestBuilder = TruKKerUpdateRequest.builder()
                    .orderId(orderId)
                    .status(status)
                    .driverPhone(driverPhone)
                    .timestamp(getCurrentTimestamp());

            if (newEta != null && !newEta.isEmpty()) {
                requestBuilder.newEta(newEta);
            }

            if (reason != null && !reason.isEmpty()) {
                requestBuilder.reason(reason);
            }

            TruKKerUpdateRequest request = requestBuilder.build();

            String endpoint = newEta != null && !newEta.isEmpty() ? 
                    "/api/shipments/updateEta" : "/api/shipments/updateStatus";

            sendUpdateToTruKKer(request, endpoint);
            log.info("Updated TruKKer system - Status: {} for order: {}", status, orderId);
        } catch (Exception e) {
            log.error("Error updating TruKKer system: {}", e.getMessage(), e);
        }
    }

    /**
     * Log what would be sent to TruKKer API (API integration not implemented yet)
     */
    private void sendUpdateToTruKKer(TruKKerUpdateRequest request, String endpoint) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            
            log.info("=== TruKKer API Update (SIMULATION) ===");
            log.info("Endpoint: {}", endpoint);
            log.info("Request Body: {}", jsonBody);
            log.info("Note: TruKKer API integration not implemented yet. This is a simulation.");
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("Error creating TruKKer update request: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current timestamp in ISO format
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
} 