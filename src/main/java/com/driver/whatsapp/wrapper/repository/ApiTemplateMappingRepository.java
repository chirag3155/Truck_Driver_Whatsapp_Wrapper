package com.driver.whatsapp.wrapper.repository;

import com.driver.whatsapp.wrapper.entity.ApiTemplateMapping;
import com.driver.whatsapp.wrapper.entity.ApiTemplateMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTemplateMappingRepository extends JpaRepository<ApiTemplateMapping, ApiTemplateMappingId> {
    
    /**
     * Find template by API name, language, and template name
     */
    @Query("SELECT atm FROM ApiTemplateMapping atm WHERE atm.id.apiName = :apiName AND atm.id.lang = :lang AND atm.id.templateName = :templateName AND atm.isActive = true")
    Optional<ApiTemplateMapping> findByApiNameAndLangAndTemplateName(
        @Param("apiName") String apiName, 
        @Param("lang") String lang, 
        @Param("templateName") String templateName);
    
    /**
     * Find all templates for a specific API name and language
     */
    @Query("SELECT atm FROM ApiTemplateMapping atm WHERE atm.id.apiName = :apiName AND atm.id.lang = :lang AND atm.isActive = true")
    Optional<ApiTemplateMapping> findByApiNameAndLang(@Param("apiName") String apiName, @Param("lang") String lang);
    
    /**
     * Find all templates for a specific API name
     */
    @Query("SELECT atm FROM ApiTemplateMapping atm WHERE atm.id.apiName = :apiName AND atm.isActive = true")
    List<ApiTemplateMapping> findByApiName(@Param("apiName") String apiName);
    
    /**
     * Find all templates for a specific language
     */
    @Query("SELECT atm FROM ApiTemplateMapping atm WHERE atm.id.lang = :lang AND atm.isActive = true")
    List<ApiTemplateMapping> findByLang(@Param("lang") String lang);
    
    /**
     * Find all active templates
     */
    @Query("SELECT atm FROM ApiTemplateMapping atm WHERE atm.isActive = true")
    List<ApiTemplateMapping> findAllActive();
    
    /**
     * Check if template exists for API name, language, and template name
     */
    @Query("SELECT COUNT(atm) > 0 FROM ApiTemplateMapping atm WHERE atm.id.apiName = :apiName AND atm.id.lang = :lang AND atm.id.templateName = :templateName AND atm.isActive = true")
    boolean existsByApiNameAndLangAndTemplateName(
        @Param("apiName") String apiName, 
        @Param("lang") String lang, 
        @Param("templateName") String templateName);
} 