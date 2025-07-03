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
public class ReminderRequest {
    
    @NotBlank(message = "Driver phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Driver phone must be 10-15 digits")
    @JsonProperty("driverPhone")
    private String driverPhone;
    
    @NotBlank(message = "Driver name is required")
    @JsonProperty("driverName")
    private String driverName;
    
    @NotBlank(message = "Drop off location is required")
    @JsonProperty("dropOff")
    private String dropOff;
    
    @NotBlank(message = "Pick up location is required")
    @JsonProperty("pickUp")
    private String pickUp;
    
    @NotBlank(message = "Commodity is required")
    @JsonProperty("commodity")
    private String commodity;
    
    @NotNull(message = "Move date is required")
    @JsonProperty("moveDate")
    private LocalDateTime moveDate;
    
    @NotBlank(message = "Order ID is required")
    @JsonProperty("orderId")
    private String orderId;
    
    @NotBlank(message = "Truck number is required")
    @JsonProperty("truckNumber")
    private String truckNumber;
    
    @NotNull(message = "ETA time is required")
    @JsonProperty("etaTime")
    private LocalDateTime etaTime;
    
    // Reference ID will be generated on the server side
    @JsonProperty("referenceId")
    private String referenceId;
} 