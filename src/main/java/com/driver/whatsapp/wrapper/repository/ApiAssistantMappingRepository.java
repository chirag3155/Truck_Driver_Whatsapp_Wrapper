package com.driver.whatsapp.wrapper.repository;

import com.driver.whatsapp.wrapper.entity.ApiAssistantMapping;
import com.driver.whatsapp.wrapper.entity.ApiAssistantMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiAssistantMappingRepository extends JpaRepository<ApiAssistantMapping, ApiAssistantMappingId> {
    
    /**
     * Find mapping by API name and communication mode
     */
    @Query("SELECT am FROM ApiAssistantMapping am WHERE am.apiName = :apiName AND am.communicationMode = :communicationMode")
    Optional<ApiAssistantMapping> findByApiNameAndCommunicationMode(@Param("apiName") String apiName, @Param("communicationMode") String communicationMode);
    
    /**
     * Find all mappings for a specific API name
     */
    @Query("SELECT am FROM ApiAssistantMapping am WHERE am.apiName = :apiName")
    List<ApiAssistantMapping> findByApiName(@Param("apiName") String apiName);
    
    /**
     * Find all mappings for a specific communication mode
     */
    @Query("SELECT am FROM ApiAssistantMapping am WHERE am.communicationMode = :communicationMode")
    List<ApiAssistantMapping> findByCommunicationMode(@Param("communicationMode") String communicationMode);
    
    /**
     * Find all mappings for a specific tenant
     */
    List<ApiAssistantMapping> findByTenantId(String tenantId);
    
    /**
     * Find all mappings for a specific assistant
     */
    List<ApiAssistantMapping> findByAssistantId(String assistantId);
    
    /**
     * Check if mapping exists for API name and communication mode
     */
    @Query("SELECT COUNT(am) > 0 FROM ApiAssistantMapping am WHERE am.id.apiName = :apiName AND am.id.communicationMode = :communicationMode")
    boolean existsByApiNameAndCommunicationMode(@Param("apiName") String apiName, @Param("communicationMode") String communicationMode);
} 