package com.driver.whatsapp.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowResponse {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("truckNumber")
    private String truckNumber;

    @JsonProperty("uniqueId")
    private String uniqueId;

    @JsonProperty("tripId")
    private String tripId;

    @JsonProperty("flowName")
    private String flowName;

}
