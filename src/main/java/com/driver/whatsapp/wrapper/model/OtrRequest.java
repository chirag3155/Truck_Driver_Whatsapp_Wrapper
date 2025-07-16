package com.driver.whatsapp.wrapper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtrRequest {
    
    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("driverName")
    private String driverName;
    
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("truckNumber")
    private String truckNumber;
    
    @JsonProperty("commodity")
    private String commodity; // Can be null
    
    @JsonProperty("dropOffLocation")
    private String dropOffLocation;
    
    @JsonProperty("eta")
    private String eta;
    
    @JsonProperty("orderDate")
    private String orderDate;
    
    @JsonProperty("pickUpLocation")
    private String pickUpLocation;
    
    @JsonProperty("tripId")
    private String tripId;

    @JsonProperty("numberOfTrips")
    private String numberOfTrips;
    
    @JsonProperty("uniqueId")
    private String uniqueId;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("assistantId")
    private String assistantId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("conversationId")
    private String conversationId;

    @JsonProperty("lang")
    private String lang;
    
    // These will be generated server-side
    // @JsonProperty("conversationId")
    // private String conversationId;
    
    // @JsonProperty("transactionId")
    // private String transactionId;

    // Getters
    public String getOrderDate() {
        return orderDate;
    }
    
    public String getEta() {
        return eta;
    }
} 