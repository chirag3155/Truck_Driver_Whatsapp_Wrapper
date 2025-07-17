package com.driver.whatsapp.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class ConversationMessageDto {
    private String type;
    private String content;
    private String timestamp;  // ISO-8601 UTC timestamp
    private String correlationId;
} 