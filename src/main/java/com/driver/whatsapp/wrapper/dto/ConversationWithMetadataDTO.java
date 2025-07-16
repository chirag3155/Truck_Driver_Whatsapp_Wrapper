package com.driver.whatsapp.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationWithMetadataDTO {
    private String conversationId;
    private String phoneNumber;
    private String truckNumber;
    private String orderNumber;
    private String transactionId;
    private List<ConversationMessageDto> messages;
}