package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.model.FlowType;
import com.driver.whatsapp.wrapper.model.OtrRequest;
import com.driver.whatsapp.wrapper.service.OtrService;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wawrapper")
@Slf4j
@Tag(name = "Dynamic Flow Management", description = "APIs for dynamic flow management including OTR, Loading Confirmation, Order Completion, Reminder, and Status Follow-up")
public class OtrController {

    @Autowired
    private OtrService otrService;

    /**
     * Generate dynamic flow conversation and send message to driver
     */
    @Operation(
        summary = "Generate Dynamic Flow Conversation",
        description = "Creates a new conversation for any flow type (OTR, Loading Confirmation, Order Completion, Reminder, Status Follow-up), " +
                     "saves transaction details, calls chat module for response generation, and sends message to driver via specified communication mode"
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
                      "status": "success",
                      "message": "OTR request processed successfully",
                      "data": {
                        "conversationId": "CON1234567890ABC",
                        "transactionId": "A1B2C3D4E5F67890",
                        "orderNumber": "ORD987654321",
                        "truckNumber": "TRK001",
                        "phoneNumber": "971526328601",
                        "tripId": "TRIP123",
                        "uniqueId": "UNIQUE456",
                        "chatResponse": "Hello! Your delivery details have been updated...",
                        "messageSent": true,
                        "statusCode": "MESSAGE_SENT",
                        "communicationMode": "whatsapp",
                        "flowName": "OTR",
                        "timestamp": "2025-01-20T10:30:00"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid flow request",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "error",
                      "message": "Invalid flow type: INVALID_FLOW",
                      "data": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "error",
                      "message": "Failed to process OTR request: Database connection error",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/{flow_name}/generate")
    public ResponseEntity<Map<String, Object>> generateFlow(
        @PathVariable("flow_name") String flowName,
        @RequestParam(value = "communication_mode", defaultValue = "whatsapp") String communicationMode,
        @Valid @RequestBody OtrRequest otrRequest) {
        try {
            log.info("Received {} flow generate request for order: {}, truck: {}, phone: {}, communication_mode: {}", 
                    flowName, otrRequest.getOrderNumber(), otrRequest.getTruckNumber(), otrRequest.getPhoneNumber(), communicationMode);

            // Validate flow type
            if (!FlowType.isValid(flowName)) {
                log.error("Invalid flow type: {}", flowName);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Invalid flow type: " + flowName + ". Valid types: " + 
                    String.join(", ", FlowType.LOADING_CONFIRMATION.getValue(), FlowType.ORDER_COMPLETION.getValue(), 
                               FlowType.OTR.getValue(), FlowType.REMINDER.getValue(), FlowType.STATUS_FOLLOW_UP.getValue()));
                errorResponse.put("data", null);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Process flow request
            Map<String, Object> result = otrService.processFlowRequest(otrRequest, flowName, communicationMode);
            
            // Determine HTTP status based on result
            if ("success".equals(result.get("status"))) {
                log.info("{} flow request processed successfully for order: {}", flowName, otrRequest.getOrderNumber());
                return ResponseEntity.ok(result);
            } else {
                log.error("{} flow request processing failed for order: {}", flowName, otrRequest.getOrderNumber());
                return ResponseEntity.status(500).body(result);
            }

        } catch (Exception e) {
            log.error("Error processing {} flow generate request: {}", flowName, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to process " + flowName + " request: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 