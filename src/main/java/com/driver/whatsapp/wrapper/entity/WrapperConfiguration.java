package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "wrapper_configuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrapperConfiguration {
    
    @EmbeddedId
    private WrapperConfigurationId id;
    
    @Column(name = "param_name", nullable = false, length = 200)
    private String paramName;
   
    @Column(name = "param_value", nullable = false, columnDefinition = "TEXT")
    private String paramValue;
    
} 