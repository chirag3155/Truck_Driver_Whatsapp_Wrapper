package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.dto.ApiGenericResponse;
import com.driver.whatsapp.wrapper.dto.ConversationWithMetadataDTO;
import com.driver.whatsapp.wrapper.service.DriverElasticService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wawrapper/elastic")
@Tag(name = "Driver Elastic Controller", description = "APIs for retrieving driver conversations from Elasticsearch")
@Slf4j
public class DriverElasticController {

    private final DriverElasticService driverElasticService;

    @Autowired
    public DriverElasticController(DriverElasticService driverElasticService) {
        this.driverElasticService = driverElasticService;
    }

    @GetMapping("/conversation/{transactionId}")
    @Operation(
        summary = "Get conversations by transaction ID",
        description = "Retrieves all conversations associated with a specific transaction ID from Elasticsearch"
    )
    public ResponseEntity<ApiGenericResponse<List<ConversationWithMetadataDTO>>> getConversationsByTransactionId(
            @PathVariable String transactionId) {
        
        log.info("Received request to get conversations for transactionId: {}", transactionId);
        
        try {
            List<ConversationWithMetadataDTO> conversations = driverElasticService.getConversationsWithMetadataByTransactionId(transactionId);
            
            log.info("Successfully retrieved {} conversations for transactionId: {}", 
                    conversations.size(), transactionId);
                    
            // Log conversation details for debugging
            for (int i = 0; i < conversations.size(); i++) {
                ConversationWithMetadataDTO conv = conversations.get(i);
                log.debug("Conversation {}: ID={}, Messages={}, Phone={}, Truck={}", 
                        i + 1, conv.getConversationId(), conv.getMessages().size(), 
                        conv.getPhoneNumber(), conv.getTruckNumber());
            }
            
            return ResponseEntity.ok(ApiGenericResponse.<List<ConversationWithMetadataDTO>>builder()
                    .status(200)
                    .message("Conversations retrieved successfully")
                    .data(conversations)
                    .build());
                    
        } catch (IOException e) {
            log.error("IOException while retrieving conversations for transactionId: {}. Error: {}", 
                    transactionId, e.getMessage(), e);
            return ResponseEntity.ok(ApiGenericResponse.<List<ConversationWithMetadataDTO>>builder()
                    .status(500)
                    .message("Failed to retrieve conversations: " + e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Unexpected error while retrieving conversations for transactionId: {}. Error: {}", 
                    transactionId, e.getMessage(), e);
            return ResponseEntity.ok(ApiGenericResponse.<List<ConversationWithMetadataDTO>>builder()
                    .status(500)
                    .message("Internal server error: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @Operation(
        summary = "Create a new conversation",
        description = "Creates a new conversation document in Elasticsearch",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping
    public ResponseEntity<ApiGenericResponse<JsonNode>> createConversation(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @RequestBody JsonNode conversation) {
            
        log.info("Received request to create conversation");
        log.debug("Request payload size: {} characters", conversation.toString().length());
        
        try {
            // Extract user info for logging if available
            String userId = "unknown";
            String sessionId = "unknown";
            String conversationId = "unknown";
            
            if (conversation != null) {
                JsonNode userInfo = conversation.get("userInfo");
                if (userInfo != null && userInfo.get("id") != null) {
                    userId = userInfo.get("id").asText();
                }
                
                JsonNode sessionNode = conversation.get("sessionId");
                if (sessionNode != null) {
                    sessionId = sessionNode.asText();
                }
                
                JsonNode convIdNode = conversation.get("conversationId");
                if (convIdNode != null) {
                    conversationId = convIdNode.asText();
                }
            }
            
            log.info("Creating conversation - ID: {}, SessionID: {}, UserID: {}", 
                    conversationId, sessionId, userId);

            JsonNode createdConversation = driverElasticService.createConversation(conversation);

            log.info("Successfully created conversation in Elasticsearch");
            log.debug("Created conversation response: {}", createdConversation.toString());

            return ResponseEntity.ok(ApiGenericResponse.<JsonNode>builder()
                    .status(200)
                    .message("Conversation created successfully")
                    .data(createdConversation)
                    .build());
        } catch (Exception e) {
            log.error("Error creating conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiGenericResponse.<JsonNode>builder()
                            .status(500)
                            .message("Failed to create conversation: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }
} 