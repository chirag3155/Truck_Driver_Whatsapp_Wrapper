package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Composite primary key for TransactionDetail entity.
 * Must match the @Id fields in both name and type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TransactionDetailId implements Serializable {
    private String phoneNumber;
    private String transactionId;
    private String truckNumber;
    private String orderNumber;
} 