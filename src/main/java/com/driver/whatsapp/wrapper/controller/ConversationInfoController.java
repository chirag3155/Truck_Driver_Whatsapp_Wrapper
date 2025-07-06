package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.dto.ConversationInfoRequest;
import com.driver.whatsapp.wrapper.dto.ConversationInfoResponse;
import com.driver.whatsapp.wrapper.service.ConversationInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/wawrapper/conversation")
@Slf4j
@Validated
public class ConversationInfoController {

    @Autowired
    private ConversationInfoService conversationInfoService;

    /**
     * Get conversation information by conversation ID
     * GET /api/v1/conversation/{conversationId}/info
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversationInfo(
            @PathVariable("conversationId") @NotBlank(message = "Conversation ID is required") String conversationId) {
        
        log.info("📞 API Request: Get conversation info for conversation ID: {}", conversationId);
        
        try {
            ConversationInfoRequest request = ConversationInfoRequest.builder()
                .conversationId(conversationId)
                .build();
                
            ConversationInfoResponse response = conversationInfoService.getConversationInfo(request);
            
            if (response.isFound()) {
                log.info("✅ API Response: Conversation info retrieved successfully for ID: {}", conversationId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("⚠️ API Response: Conversation not found for ID: {}", conversationId);
                // Check if this is a default response (has default values) or a simple not found
                if (response.getDriverName() != null) {
                    // This is a default response
                    return ResponseEntity.ok(response);
                } else {
                    // This is a simple not found - return just message
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "No conversation found for ID: " + conversationId);
                    return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
                }
            }
        } catch (Exception e) {
            log.error("❌ API Error: Failed to get conversation info for ID {}: {}", conversationId, e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
        }
    }

    /**
     * Get conversation information by phone number (most recent open conversation)
     * GET /api/v1/conversation/phone/{phoneNumber}/info
     */
    @GetMapping("/phone/{phoneNumber}/info")
    public ResponseEntity<?> getConversationInfoByPhone(
            @PathVariable("phoneNumber") 
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^[0-9+\\-\\s()]{10,15}$", message = "Invalid phone number format")
            String phoneNumber) {
        
        log.info("📞 API Request: Get conversation info for phone number: {}", phoneNumber);
        
        try {
            ConversationInfoResponse response = conversationInfoService.getConversationInfoByPhoneNumber(phoneNumber);
            
            if (response.isFound()) {
                log.info("✅ API Response: Conversation info retrieved successfully for phone: {}", phoneNumber);
                return ResponseEntity.ok(response);
            } else {
                log.warn("⚠️ API Response: No open conversation found for phone: {}", phoneNumber);
                // Check if this is a default response (has default values) or a simple not found
                if (response.getDriverName() != null) {
                    // This is a default response
                    return ResponseEntity.ok(response);
                } else {
                    // This is a simple not found - return just message
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "No open conversation found for phone: " + phoneNumber);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }
            }
        } catch (Exception e) {
            log.error("❌ API Error: Failed to get conversation info for phone {}: {}", phoneNumber, e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/conversation/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("🏥 Health check requested");
        return ResponseEntity.ok("Conversation Info API is healthy");
    }
} 