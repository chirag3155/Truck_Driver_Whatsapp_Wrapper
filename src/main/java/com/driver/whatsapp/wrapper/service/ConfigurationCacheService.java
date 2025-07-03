package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.entity.WrapperConfiguration;
import com.driver.whatsapp.wrapper.entity.ApiAssistantMapping;
import com.driver.whatsapp.wrapper.repository.WrapperConfigurationRepository;
import com.driver.whatsapp.wrapper.repository.ApiAssistantMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ConfigurationCacheService {

    @Autowired
    private WrapperConfigurationRepository configurationRepository;
    
    @Autowired
    private ApiAssistantMappingRepository apiAssistantMappingRepository;

    @Value("${module.name:trukker_wrapper}")
    private String moduleName;

    // Static thread-safe HashMap for configuration cache - accessible throughout application
    private static final Map<String, String> configurationCache = new ConcurrentHashMap<>();
    
    // Static thread-safe HashMap for API assistant mapping cache - key: "apiName_communicationMode", value: mapping object
    private static final Map<String, ApiAssistantMapping> apiAssistantMappingCache = new ConcurrentHashMap<>();
    
    // Static module name for global access
    private static String staticModuleName;

    /**
     * Load configurations from database into cache on application startup
     */
    @PostConstruct
    public void loadConfigurationsOnStartup() {
        try {
            // Set static module name for global access
            staticModuleName = moduleName;
            
            log.info("Loading configurations for module: {} on application startup", moduleName);
            
            // Load wrapper configurations
            loadWrapperConfigurations();
            
            // Load API assistant mappings
            loadApiAssistantMappings();
                     
        } catch (Exception e) {
            log.error("Error loading configurations for module: {} - {}", moduleName, e.getMessage(), e);
        }
    }
    
    /**
     * Load wrapper configurations into cache
     */
    private void loadWrapperConfigurations() {
        try {
            // Fetch all configurations for the module from database
            List<WrapperConfiguration> configurations = configurationRepository.findByModuleName(moduleName);
            
            if (configurations.isEmpty()) {
                log.warn("No wrapper configurations found for module: {}", moduleName);
            } else {
                // Clear existing cache
                configurationCache.clear();

                // Populate cache with param_id -> param_value mapping
                for (WrapperConfiguration config : configurations) {
                    String paramId = config.getId().getParamId();
                    String paramValue = config.getParamValue();
                    
                    configurationCache.put(paramId, paramValue);
                    log.debug("Loaded wrapper configuration: {} = {}", paramId, paramValue);
                }

                log.info("Successfully loaded {} wrapper configurations for module: {}", 
                         configurationCache.size(), moduleName);
            }
        } catch (Exception e) {
            log.error("Error loading wrapper configurations: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Load API assistant mappings into cache
     */
    private void loadApiAssistantMappings() {
        try {
            // Fetch all API assistant mappings from database
            List<ApiAssistantMapping> mappings = apiAssistantMappingRepository.findAll();
            
            if (mappings.isEmpty()) {
                log.warn("No API assistant mappings found");
            } else {
                // Clear existing cache
                apiAssistantMappingCache.clear();

                // Populate cache with combinedKey -> mapping object
                for (ApiAssistantMapping mapping : mappings) {
                    String combinedKey = mapping.getCombinedKey();
                    apiAssistantMappingCache.put(combinedKey, mapping);
                    log.debug("Loaded API assistant mapping: {} -> tenant: {}, assistant: {}, nextCommunicationMode: {}", 
                             combinedKey, mapping.getTenantId(), mapping.getAssistantId(), mapping.getNextCommunicationMode());
                }

                log.info("Successfully loaded {} API assistant mappings", apiAssistantMappingCache.size());
            }
        } catch (Exception e) {
            log.error("Error loading API assistant mappings: {}", e.getMessage(), e);
        }
    }

    /**
     * Get configuration value by parameter ID - STATIC ACCESS
     */
    public static String getConfigValue(String paramId) {
        return configurationCache.get(paramId);
    }

    /**
     * Get configuration value with default fallback - STATIC ACCESS
     */
    public static String getConfigValue(String paramId, String defaultValue) {
        return configurationCache.getOrDefault(paramId, defaultValue);
    }

    /**
     * Check if configuration exists - STATIC ACCESS
     */
    public static boolean hasConfig(String paramId) {
        return configurationCache.containsKey(paramId);
    }

    /**
     * Get all configuration keys - STATIC ACCESS
     */
    public static java.util.Set<String> getConfigKeys() {
        return configurationCache.keySet();
    }

    /**
     * Get all configurations as a read-only map - STATIC ACCESS
     */
    public static Map<String, String> getAllConfigurations() {
        return new HashMap<>(configurationCache);
    }

    /**
     * Reload configurations from database (manual refresh)
     */
    public void reloadConfigurations() {
        log.info("Manually reloading all configurations for module: {}", moduleName);
        loadConfigurationsOnStartup();
    }
    
    // ==================== API ASSISTANT MAPPING STATIC METHODS ====================
    
    /**
     * Get API assistant mapping by API name and communication mode - STATIC ACCESS
     */
    public static ApiAssistantMapping getApiAssistantMapping(String apiName, String communicationMode) {
        String combinedKey = apiName + "_" + communicationMode;
        return apiAssistantMappingCache.get(combinedKey);
    }
    
    /**
     * Get tenant ID for API name and communication mode - STATIC ACCESS
     */
    public static String getTenantId(String apiName, String communicationMode) {
        ApiAssistantMapping mapping = getApiAssistantMapping(apiName, communicationMode);
        return mapping != null ? mapping.getTenantId() : null;
    }
    
    /**
     * Get assistant ID for API name and communication mode - STATIC ACCESS
     */
    public static String getAssistantId(String apiName, String communicationMode) {
        ApiAssistantMapping mapping = getApiAssistantMapping(apiName, communicationMode);
        return mapping != null ? mapping.getAssistantId() : null;
    }
    
    /**
     * Get next communication mode for API name and communication mode - STATIC ACCESS
     */
    public static String getNextCommunicationMode(String apiName, String communicationMode) {
        ApiAssistantMapping mapping = getApiAssistantMapping(apiName, communicationMode);
        return mapping != null ? mapping.getNextCommunicationMode() : null;
    }
    
    /**
     * Check if API assistant mapping exists - STATIC ACCESS
     */
    public static boolean hasApiAssistantMapping(String apiName, String communicationMode) {
        String combinedKey = apiName + "_" + communicationMode;
        return apiAssistantMappingCache.containsKey(combinedKey);
    }
    
    /**
     * Get all API assistant mappings as read-only map - STATIC ACCESS
     */
    public static Map<String, ApiAssistantMapping> getAllApiAssistantMappings() {
        return new HashMap<>(apiAssistantMappingCache);
    }
    
    /**
     * Get API assistant mapping cache size - STATIC ACCESS
     */
    public static int getApiAssistantMappingCacheSize() {
        return apiAssistantMappingCache.size();
    }
    
    /**
     * Get all combined keys for API assistant mappings - STATIC ACCESS
     */
    public static java.util.Set<String> getApiAssistantMappingKeys() {
        return apiAssistantMappingCache.keySet();
    }

    /**
     * Get the current module name - STATIC ACCESS
     */
    public static String getModuleName() {
        return staticModuleName;
    }

    /**
     * Get cache size - STATIC ACCESS
     */
    public static int getCacheSize() {
        return configurationCache.size();
    }
    
    /**
     * Static method to check if cache is initialized - STATIC ACCESS
     */
    public static boolean isCacheInitialized() {
        return !configurationCache.isEmpty() && staticModuleName != null;
    }
} 