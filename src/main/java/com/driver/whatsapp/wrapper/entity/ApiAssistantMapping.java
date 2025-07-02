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
    
    // Convenience constructor
    public ApiAssistantMapping(String apiName, String communicationMode, String tenantId, String assistantId) {
        this.id = new ApiAssistantMappingId(apiName, communicationMode);
        this.tenantId = tenantId;
        this.assistantId = assistantId;
    }
    
    // Convenience getters for composite key fields
    public String getApiName() {
        return id != null ? id.getApiName() : null;
    }
    
    public String getCommunicationMode() {
        return id != null ? id.getCommunicationMode() : null;
    }
    
    // Convenience setters for composite key fields
    public void setApiName(String apiName) {
        if (this.id == null) {
            this.id = new ApiAssistantMappingId();
        }
        this.id.setApiName(apiName);
    }
    
    public void setCommunicationMode(String communicationMode) {
        if (this.id == null) {
            this.id = new ApiAssistantMappingId();
        }
        this.id.setCommunicationMode(communicationMode);
    }
    
    // Utility method to get the combined key for HashMap
    public String getCombinedKey() {
        return getApiName() + "_" + getCommunicationMode();
    }
} 