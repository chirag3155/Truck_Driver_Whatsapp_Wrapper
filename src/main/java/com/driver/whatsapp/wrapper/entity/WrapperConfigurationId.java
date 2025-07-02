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
public class WrapperConfigurationId implements Serializable {
    
    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;
    
    @Column(name = "param_id", nullable = false, length = 100)
    private String paramId;
} 