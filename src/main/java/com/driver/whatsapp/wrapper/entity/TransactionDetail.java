package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transaction_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetail {
    
    @EmbeddedId
    private TransactionDetailId id;
    
    @Column(name = "driver_name", nullable = false, length = 255)
    private String driverName;
    
    @Column(name = "drop_off_location", nullable = false, length = 500)
    private String dropOffLocation;
    
    @Column(name = "pick_up_location", nullable = false, length = 500)
    private String pickUpLocation;
    
    @Column(name = "commodity", nullable = true, length = 255)
    private String commodity;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "eta_time", nullable = true)
    private LocalDateTime etaTime;

    @Column(name = "new_eta", nullable = true)
    private LocalDateTime newETA;
    
    @Column(name = "unique_id", nullable = false)
    private String uniqueId;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(name = "conversation_id", nullable = false)
    private String ConversationId;

    @Column(name = "client_name", nullable = true)
    private String clientName;

    @Column(name = "flow_name", nullable = true)
    private String flowName;

    @Column(name = "status_code", nullable = true)
    private String statusCode;

    @Column(name = "communication_mode", nullable = false)
    private String communicationMode;

    @Column(name = "state", nullable = true)
    private String state;
    
    @CreationTimestamp
    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp;
    
    @Column(name = "updated_timestamp")
    private LocalDateTime updatedTimestamp;
    
    @PrePersist
    protected void onCreate() {
        transactionTimestamp = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedTimestamp = LocalDateTime.now();
    }
    
    /**
     * Convenience method to get truck number
     */
    public String getTruckNumber() {
        return id != null ? id.getTruckNumber() : null;
    }
    
    /**
     * Convenience method to get order number
     */
    public String getOrderNumber() {
        return id != null ? id.getOrderNumber() : null;
    }
    
    /**
     * Convenience method to get phone number
     */
    public String getPhoneNumber() {
        return id != null ? id.getPhoneNumber() : null;
    }
    
    /**
     * Convenience method to get transaction ID
     */
    public String getTransactionId() {
        return id != null ? id.getTransactionId() : null;
    }
} 