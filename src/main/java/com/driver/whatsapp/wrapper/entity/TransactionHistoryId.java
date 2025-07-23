
package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TransactionHistoryId implements Serializable {
    private String phoneNumber;
    private String transactionId;
    private String conversationId;
    private String statusCode;
} 