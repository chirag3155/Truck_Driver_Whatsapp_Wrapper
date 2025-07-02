package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApiAssistantMappingId implements Serializable {
    
    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;
    
    @Column(name = "communication_mode", nullable = false, length = 50)
    private String communicationMode;
} 