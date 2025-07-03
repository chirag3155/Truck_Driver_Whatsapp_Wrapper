package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Builder;

import jakarta.persistence.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class TransactionDetailId implements Serializable {
    
    @Column(name = "truck_number", nullable = false, length = 50)
    private String truckNumber;
    
    @Column(name = "order_number", nullable = false, length = 100)
    private String orderNumber;
    
    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;
    
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;
} 