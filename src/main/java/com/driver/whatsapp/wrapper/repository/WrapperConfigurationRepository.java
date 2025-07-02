package com.driver.whatsapp.wrapper.repository;

import com.driver.whatsapp.wrapper.entity.WrapperConfiguration;
import com.driver.whatsapp.wrapper.entity.WrapperConfigurationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WrapperConfigurationRepository extends JpaRepository<WrapperConfiguration, WrapperConfigurationId> {
    
    /**
     * Find all configurations for a specific module
     */
    @Query("SELECT wc FROM WrapperConfiguration wc WHERE wc.id.moduleName = :moduleName")
    List<WrapperConfiguration> findByModuleName(@Param("moduleName") String moduleName);
    
    /**
     * Find a specific configuration by module name and parameter ID
     */
    @Query("SELECT wc FROM WrapperConfiguration wc WHERE wc.id.moduleName = :moduleName AND wc.id.paramId = :paramId")
    Optional<WrapperConfiguration> findByModuleNameAndParamId(@Param("moduleName") String moduleName, @Param("paramId") String paramId);
    

    
    /**
     * Find configurations by parameter name (partial match)
     */
    List<WrapperConfiguration> findByParamNameContainingIgnoreCase(String paramName);
    
    /**
     * Check if a configuration exists for a module and parameter
     */
    @Query("SELECT COUNT(wc) > 0 FROM WrapperConfiguration wc WHERE wc.id.moduleName = :moduleName AND wc.id.paramId = :paramId")
    boolean existsByModuleNameAndParamId(@Param("moduleName") String moduleName, @Param("paramId") String paramId);
} 