package com.driver.whatsapp.wrapper.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationInfoRequest {
    
    @NotNull(message = "Conversation ID is required")
    @NotBlank(message = "Conversation ID cannot be blank")
    private String conversationId;
} 