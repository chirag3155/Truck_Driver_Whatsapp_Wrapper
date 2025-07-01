package com.driver.whatsapp.wrapper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruKKerUpdateRequest {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("status")
    private String status; // on_time, delayed, breakdown
    
    @JsonProperty("newEta")
    private String newEta;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("driverPhone")
    private String driverPhone;
    
    @JsonProperty("timestamp")
    private String timestamp;
} 