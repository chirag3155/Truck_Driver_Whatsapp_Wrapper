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
    
    @NotBlank(message = "Order number is required")
    @JsonProperty("orderNumber")
    private String orderNumber;

    @NotBlank(message = "Driver name is required")
    @JsonProperty("driverName")
    private String driverName;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @NotBlank(message = "Truck number is required")
    @JsonProperty("truckNumber")
    private String truckNumber;
    
    @JsonProperty("commodity")
    private String commodity; // Can be null
    
    @NotBlank(message = "Drop off location is required")
    @JsonProperty("dropOffLocation")
    private String dropOffLocation;
    
    @NotNull(message = "ETA is required")
    @JsonProperty("eta")
    private LocalDateTime eta;
    
    @NotNull(message = "Order date is required")
    @JsonProperty("orderDate")
    private LocalDateTime orderDate;
    
    @NotBlank(message = "Pick up location is required")
    @JsonProperty("pickUpLocation")
    private String pickUpLocation;
    
    @NotBlank(message = "Trip ID is required")
    @JsonProperty("tripId")
    private String tripId;
    
    @NotBlank(message = "Unique ID is required")
    @JsonProperty("uniqueId")
    private String uniqueId;
    
    // These will be generated server-side
    // @JsonProperty("conversationId")
    // private String conversationId;
    
    // @JsonProperty("transactionId")
    // private String transactionId;
} 