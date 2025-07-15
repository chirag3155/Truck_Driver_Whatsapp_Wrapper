package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Composite primary key for ApiAssistantMapping entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApiAssistantMappingId implements Serializable {
    private String apiName;
    private String communicationMode;
    private String lang;
} 