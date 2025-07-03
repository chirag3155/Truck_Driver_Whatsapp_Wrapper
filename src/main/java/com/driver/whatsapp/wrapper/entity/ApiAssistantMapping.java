package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "api_assistant_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiAssistantMapping {
    
    @EmbeddedId
    private ApiAssistantMappingId id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "assistant_id", nullable = false, length = 100)
    private String assistantId;
    
    @Column(name = "next_communication_mode", nullable = false, length = 50)
    private String nextCommunicationMode;
    
    /**
     * Get combined key for cache mapping (apiName_communicationMode)
     */
    public String getCombinedKey() {
        if (id != null) {
            return id.getApiName() + "_" + id.getCommunicationMode();
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
     * Convenience method to get communication mode
     */
    public String getCommunicationMode() {
        return id != null ? id.getCommunicationMode() : null;
    }
} 