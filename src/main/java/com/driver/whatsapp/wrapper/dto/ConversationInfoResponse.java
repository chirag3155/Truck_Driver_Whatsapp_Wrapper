package com.driver.whatsapp.wrapper.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

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
    private String orderDate;
    private String eta;
    private String truckNumber;
    private String orderNumber;
    private String tripId;
    private String flowName;
    private String communicationMode;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("current_datatime")
    private ZonedDateTime currentDatetime;

    // Status information retained for internal use but hidden from response
    private boolean found;

    @JsonIgnore
    private String message;
} 