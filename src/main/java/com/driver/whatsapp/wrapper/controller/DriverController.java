package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.model.DriverDetails;
import com.driver.whatsapp.wrapper.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wawrapper/")
@Slf4j
@Tag(name = "TruKKer Integration", description = "APIs for receiving driver details from TruKKer system")
public class DriverController {

    @Autowired
    private WhatsAppService whatsAppService;

    /**
     * Endpoint to receive truck driver details from TruKKer system
     * This triggers the initial WhatsApp message to the driver
     */
    @Operation(
        summary = "Receive Driver Details from TruKKer",
        description = "Receives truck driver details from TruKKer system and initiates WhatsApp communication with the driver via Infobip API. " +
                     "This endpoint is called by TruKKer when a new shipment is assigned to a driver."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Driver details received successfully and WhatsApp message sent",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "success",
                      "message": "Driver details received and WhatsApp message sent successfully",
                      "orderId": "ORD987654321",
                      "driverPhone": "971526328601",
                      "timestamp": "2025-06-25T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid driver details provided",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "error",
                      "message": "Validation failed: Driver name is required",
                      "timestamp": "2025-06-25T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error occurred while processing request",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "error",
                      "message": "Failed to process driver details: Connection timeout",
                      "timestamp": "2025-06-25T10:30:00"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/driver-details")
    public ResponseEntity<Map<String, Object>> receiveDriverDetails(@Valid @RequestBody DriverDetails driverDetails) {
        try {
            log.info("Received driver details for order: {}, Driver: {}, Phone: {}", 
                    driverDetails.getOrderId(), driverDetails.getDriverName(), driverDetails.getDriverPhone());

            // Send initial WhatsApp message to driver
            whatsAppService.sendInitialDriverMessage(driverDetails);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Driver details received and WhatsApp message sent successfully");
            response.put("orderId", driverDetails.getOrderId());
            response.put("driverPhone", driverDetails.getDriverPhone());
            response.put("timestamp", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing driver details: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to process driver details: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(200).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @Operation(
        summary = "Health Check",
        description = "Returns the health status of the Truck Driver WhatsApp Wrapper service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy and running",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "UP",
                      "service": "Truck Driver WhatsApp Wrapper",
                      "timestamp": "2025-06-25T10:30:00"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Truck Driver WhatsApp Wrapper");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
} 