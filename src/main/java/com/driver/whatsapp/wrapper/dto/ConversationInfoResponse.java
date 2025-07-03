package com.driver.whatsapp.wrapper.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationInfoResponse {
    
    private String conversationId;
    private String driverName;
    private String phoneNumber;
    private String source;
    private String destination;
    private LocalDateTime eta;
    private String truckNumber;
    private String orderNumber;
    private String tripId;
    private String flowName;
    private String communicationMode;
    private String state;
    private LocalDateTime lastActivity;
    
    // Status information
    private boolean found;
    private String message;
} 