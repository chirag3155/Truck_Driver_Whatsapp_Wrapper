package com.driver.whatsapp.wrapper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Driver details received from TruKKer system")
public class DriverDetails {
    
    @NotBlank(message = "Driver name is required")
    @JsonProperty("driverName")
    @Schema(description = "Full name of the truck driver", example = "Ahmed Hassan", required = true)
    private String driverName;
    
    @NotBlank(message = "Pickup location is required")
    @JsonProperty("pickup")
    @Schema(description = "Pickup location for the shipment", example = "Dubai Marina", required = true)
    private String pickup;
    
    @NotBlank(message = "Dropoff location is required")
    @JsonProperty("dropoff")
    @Schema(description = "Drop-off location for the shipment", example = "Abu Dhabi Downtown", required = true)
    private String dropoff;
    
    @NotBlank(message = "Move date is required")
    @JsonProperty("moveDate")
    @Schema(description = "Scheduled date for the shipment", example = "26th June 2025", required = true)
    private String moveDate;
    
    @JsonProperty("commodity")
    @Schema(description = "Type of goods being transported", example = "Electronics & Consumer Goods")
    private String commodity;
    
    @NotBlank(message = "Order ID is required")
    @JsonProperty("orderId")
    @Schema(description = "Unique order identifier from TruKKer system", example = "ORD987654321", required = true)
    private String orderId;
    
    @JsonProperty("customerName")
    @Schema(description = "Name of the customer/company", example = "Al Futtaim Logistics")
    private String customerName;
    
    @NotBlank(message = "ETA time is required")
    @JsonProperty("etaTime")
    @Schema(description = "Estimated time of arrival", example = "9:30 AM", required = true)
    private String etaTime;
     
    @NotBlank(message = "Driver phone is required")
    @JsonProperty("driverPhone")
    @Schema(description = "Driver's WhatsApp phone number (including country code)", example = "971526328601", required = true)
    private String driverPhone;
} 