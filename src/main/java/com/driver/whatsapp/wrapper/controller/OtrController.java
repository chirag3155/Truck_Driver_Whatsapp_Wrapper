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
                      "status": 200,
                      "message": "Flow request processed successfully",
                      "path": "/wawrapper/otr/generate",
                      "timestamp": "2024-01-01T12:00:00",
                      "data": {
                        "transactionId": "txn_123456"
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
                      "status": 400,
                      "message": "Invalid flow type: INVALID_FLOW",
                      "path": "/wawrapper/invalid/generate",
                      "timestamp": "2024-01-01T12:00:00",
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
                      "status": 500,
                      "message": "Failed to process flow request",
                      "path": "/wawrapper/otr/generate",
                      "timestamp": "2024-01-01T12:00:00",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/{flow_name}/generate")
    public ResponseEntity<ApiGenericResponse<FlowResponse>> generateFlow(
        @PathVariable("flow_name") String flowName,
        @RequestParam(value = "communication_mode", defaultValue = "whatsapp") String communicationMode,
        @Valid @RequestBody OtrRequest otrRequest) {
        try {
            log.info("Received {} flow generate request for order: {}, truck: {}, phone: {}, communication_mode: {}", 
                    flowName, otrRequest.getOrderNumber(), otrRequest.getTruckNumber(), otrRequest.getPhoneNumber(), communicationMode);

            // Validate flow type
            if (!FlowType.isValid(flowName)) {
                log.error("Invalid flow type: {}", flowName);
                return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                    .status(400)
                    .message("Invalid flow type: " + flowName + ". Valid types: " + 
                        String.join(", ", FlowType.LOADING_CONFIRMATION.getValue(), FlowType.ORDER_COMPLETION.getValue(), 
                                   FlowType.OTR.getValue(), FlowType.REMINDER.getValue(), FlowType.STATUS_FOLLOW_UP.getValue()))
                    .path("/wawrapper/" + flowName + "/generate")
                    .data(null)
                    .build());
            }

            // Process flow request
            FlowResponse response = otrService.processFlowRequest(otrRequest, flowName, communicationMode);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(200)
                .message("Flow request processed successfully")
                .path("/wawrapper/" + flowName + "/generate")
                .data(response)
                .build());

        } catch (IllegalArgumentException e) {
            log.error("Validation error in {} flow generate request: {}", flowName, e.getMessage(), e);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(400)
                .message(e.getMessage())
                .path("/wawrapper/" + flowName + "/generate")
                .data(null)
                .build());
        } catch (RuntimeException e) {
            log.error("Error processing {} flow generate request: {}", flowName, e.getMessage(), e);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(200)
                .message(e.getMessage())
                .path("/wawrapper/" + flowName + "/generate")
                .data(null)
                .build());
        } 
        catch (Exception e) {
            log.error("Error processing {} flow generate request: {}", flowName, e.getMessage(), e);
            
            return ResponseEntity.ok(ApiGenericResponse.<FlowResponse>builder()
                .status(500)
                .message("Failed to process " + flowName + " request: " + e.getMessage())
                .path("/wawrapper/" + flowName + "/generate")
                .data(null)
                .build());
        }
    }
} 