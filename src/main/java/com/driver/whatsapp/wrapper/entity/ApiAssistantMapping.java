package com.driver.whatsapp.wrapper.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;

/**
 * Entity representing the mapping between APIs and assistant configurations.
 * <p>
 * Used to determine which assistant configuration applies to a given API and communication mode.
 *
 * @author mohd.shadab
 */
@Entity
@Table(name = "api_assistant_mapping")
@IdClass(ApiAssistantMappingId.class)
@Data
public class ApiAssistantMapping {
    @Id
    @Column(name = "api_name")
    private String apiName;

    @Id
    @Column(name = "communication_mode")
    private String communicationMode;

    @Id
    @Column(name = "lang")
    private String lang;

    @Column(name = "assistant_id")
    private String assistantId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "next_communication_mode")
    private String nextCommunicationMode;

    /**
     * Get combined key for caching purposes
     */
    public String getCombinedKey() {
        return apiName + "_" + communicationMode + "_" + lang;
    }

}