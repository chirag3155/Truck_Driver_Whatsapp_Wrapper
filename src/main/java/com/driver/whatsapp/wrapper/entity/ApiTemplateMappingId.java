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
public class ApiTemplateMappingId implements Serializable {
    
    @Column(name = "api_name", nullable = false, length = 250)
    private String apiName;
    
    @Column(name = "lang", nullable = false, length = 45)
    private String lang;
    
    @Column(name = "template_name", nullable = false, length = 45)
    private String templateName;
} 