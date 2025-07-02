package com.driver.whatsapp.wrapper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedMessage {
    
    private Long id;
    private String driverPhone;
    private String driverName;
    private String messageContent;
    private String messageId;
    private Long timestamp;
    private String messageType;
    private String platform;
    private String conversationId;
    private String failureReason;
    private Integer retryCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime lastRetryAt;
    private FailedMessageStatus status = FailedMessageStatus.PENDING;
    
    public enum FailedMessageStatus {
        PENDING,    // Ready for retry
        RETRYING,   // Currently being retried
        FAILED,     // Max retries exceeded
        PROCESSED   // Successfully processed
    }
} 