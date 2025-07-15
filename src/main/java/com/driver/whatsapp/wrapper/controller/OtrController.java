package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.model.FlowType;
import com.driver.whatsapp.wrapper.model.OtrRequest;
import com.driver.whatsapp.wrapper.service.OtrService;
import com.driver.whatsapp.wrapper.dto.ApiGenericResponse;
import com.driver.whatsapp.wrapper.dto.FlowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wawrapper")
@Slf4j
@Tag(name = "Dynamic Flow Management", description = "APIs for dynamic flow management including OTR, Loading Confirmation, Order Completion, Reminder, and Status Follow-up")
public class OtrController {

    @Autowired
    private OtrService otrService;

    @Operation(
        summary = "Generate Flow Conversation by Transaction ID",
        description = "Validates existing transaction by transaction_id, retrieves flow_name from database, " +
                     "and processes the flow conversation. Throws runtime exception if transaction_id doesn't exist."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flow conversation generated successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": 200,
                      "message": "Flow request processed successfully",
                      "path": "/wawrapper/generate",
                      "timestamp": "2024-01-01T12:00:00",
                      "data": {
                        "transactionId": "txn_123456",
                        "flowName": "OTR"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing or invalid transaction ID",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": 400,
                      "message": "Transaction ID is required in the request body",
                      "path": "/wawrapper/generate",
                      "timestamp": "2024-01-01T12:00:00",
                      "data": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Transaction not found or internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": 500,
                      "message": "Transaction not found for transaction_id: txn_invalid_123",
                      "path": "/wawrapper/generate",
                      "timestamp": "2024-01-01T12:00:00",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiGenericResponse<FlowResponse>> generateFlow(
        @Valid @RequestBody OtrRequest otrRequest) {
        try {
            // Validate transaction ID is provided
            if (otrRequest.getTransactionId() == null || otrRequest.getTransactionId().trim().isEmpty()) {
                log.error("Transaction ID is required");
                return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                    .status(400)
                    .message("Transaction ID is required in the request body")
                    .path("/wawrapper/generate")
                    .data(null)
                    .build());
            }

            String communicationMode = "whatsapp";
            log.info("Received flow generate request for transaction_id: {}, order: {}, truck: {}, phone: {}, communication_mode: {}", 
                    otrRequest.getTransactionId(), otrRequest.getOrderNumber(), otrRequest.getTruckNumber(), otrRequest.getPhoneNumber(), communicationMode);

            // Process flow request (service will get flow_name from database using transaction_id)
            FlowResponse response = otrService.processFlowRequestByTransactionId(otrRequest, communicationMode);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(200)
                .message("Flow request processed successfully")
                .path("/wawrapper/generate")
                .data(response)
                .build());

        } catch (IllegalArgumentException e) {
            log.error("Validation error in flow generate request: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(400)
                .message(e.getMessage())
                .path("/wawrapper/generate")
                .data(null)
                .build());
        } catch (RuntimeException e) {
            log.error("Runtime error processing flow generate request: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(400)
                .message(e.getMessage())
                .path("/wawrapper/generate")
                .data(null)
                .build());
        } catch (Exception e) {
            log.error("Unexpected error processing flow generate request: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(400)
                .message("Failed to process flow request: " + e.getMessage())
                .path("/wawrapper/generate")
                .data(null)
                .build());
        }
    }
} 