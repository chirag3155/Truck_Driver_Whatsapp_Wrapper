package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.model.WhatsAppWebhookResponse;
import com.driver.whatsapp.wrapper.service.MessageProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/wawrapper/whatsapp")
@Tag(name = "WhatsApp Webhooks", description = "Webhook endpoints for receiving messages from Infobip WhatsApp API")
public class WhatsAppWebhookController {

    @Autowired
    private MessageProcessingService messageProcessingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Webhook endpoint to receive inbound WhatsApp messages from drivers
     * This is called by Infobip when drivers reply to messages
     */
    @Operation(
        summary = "WhatsApp Message Webhook",
        description = "Receives inbound WhatsApp messages from drivers via Infobip webhook. " +
                     "This endpoint is called by Infobip when drivers respond to WhatsApp messages. " +
                     "The system processes the response and takes appropriate action (update status, ask for ETA, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message processed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "success",
                      "message": "Message processed successfully"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error processing message",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "error",
                      "message": "Failed to process message: JSON parsing error"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> receiveInboundMessage(@RequestBody String messageData) {
        try {
            log.info("Received WhatsApp webhook callback: {}", messageData);

            // Parse the webhook response
            WhatsAppWebhookResponse webhookResponse = objectMapper.readValue(messageData, WhatsAppWebhookResponse.class);
            
            // Process the driver's message
            if (webhookResponse.getResults() != null && !webhookResponse.getResults().isEmpty()) {
                messageProcessingService.processDriverMessage(webhookResponse);
                
                log.info("Successfully processed {} driver messages", webhookResponse.getResults().size());
            } else {
                log.warn("Received webhook with no results");
            }

            // Return success response to Infobip
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message processed successfully");
            
            return ResponseEntity.ok(response);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON parsing error for WhatsApp webhook: {}", e.getMessage());
            log.error("Raw message data: {}", messageData);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid JSON format. Expected WhatsApp webhook structure with 'results' array.");
            errorResponse.put("expectedFormat", "{ \"results\": [{ \"from\": \"phone\", \"message\": { \"text\": \"content\" } }] }");
            
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook: {}", e.getMessage(), e);
            log.error("Raw message data: {}", messageData);
            
            // Return error response
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to process message: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 