package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "api_template_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiTemplateMapping {
    
    @EmbeddedId
    private ApiTemplateMappingId id;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    
    @Column(name = "template",nullable = true, columnDefinition = "JSON")
    private String template;
    
    /**
     * Get combined key for cache mapping (apiName_lang_templateName)
     */
    public String getCombinedKey() {
        if (id != null) {
            return id.getApiName() + "_" + id.getLang() + "_" + id.getTemplateName();
        }
        return null;
    }
    
    /**
     * Convenience method to get API name
     */
    public String getApiName() {
        return id != null ? id.getApiName() : null;
    }
    
    /**
     * Convenience method to get language
     */
    public String getLang() {
        return id != null ? id.getLang() : null;
    }
    
    /**
     * Convenience method to get template name
     */
    public String getTemplateName() {
        return id != null ? id.getTemplateName() : null;
    }
} 