package com.driver.whatsapp.wrapper.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
@IdClass(TransactionHistoryId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Id
    @Column(name = "transaction_id", nullable = false, length = 20)
    private String transactionId;

    @Id
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Id
    @Column(name = "status_code", nullable = false)
    private String statusCode;


    @Column(name = "flow_name")
    private String flowName;

    @Column(name = "communication_mode")
    private String communicationMode;

    @Column(name = "state")
    private String state;

    @Column(name = "truck_number")
    private String truckNumber;

    @Column(name = "driver_name", nullable = false, length = 50)
    private String driverName;

    @Column(name = "pick_up_location", nullable = false, length = 250)
    private String pickUpLocation;

    @Column(name = "drop_off_location", nullable = false, length = 250)
    private String dropOffLocation;

    @Column(name = "transaction_timestamp", nullable = false, columnDefinition = "DATETIME(0)")
    private LocalDateTime transactionTimestamp;


    @PrePersist
    protected void onCreate() {
        transactionTimestamp = LocalDateTime.now();
    }
} 