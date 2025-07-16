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
import lombok.extern.slf4j.Slf4j;

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
    // Key: param_name (unique identifier), Value: param_value
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

                // Populate cache with param_name -> param_value mapping
                for (WrapperConfiguration config : configurations) {
                    String paramName = config.getParamName();
                    String paramValue = config.getParamValue();
                    
                    configurationCache.put(paramName, paramValue);
                    log.debug("Loaded wrapper configuration: {} = {}", paramName, paramValue);
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
     * Get configuration value by parameter name - STATIC ACCESS
     */
    public static String getConfigValue(String paramName) {
        return configurationCache.get(paramName);
    }

    /**
     * Get configuration value by parameter name with default fallback - STATIC ACCESS
     */
    public static String getConfigValue(String paramName, String defaultValue) {
        return configurationCache.getOrDefault(paramName, defaultValue);
    }

    /**
     * Check if configuration exists - STATIC ACCESS
     */
    public static boolean hasConfig(String paramName) {
        return configurationCache.containsKey(paramName);
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
     * Get API assistant mapping by API name, communication mode, and language - STATIC ACCESS
     */
    public static ApiAssistantMapping getApiAssistantMapping(String apiName, String communicationMode, String lang) {
        String combinedKey = apiName + "_" + communicationMode + "_" + lang;
        return apiAssistantMappingCache.get(combinedKey);
    }
    
    /**
     * Get API assistant mapping by API name and communication mode with default language - STATIC ACCESS
     */
    public static ApiAssistantMapping getApiAssistantMapping(String apiName, String communicationMode) {
        return getApiAssistantMapping(apiName, communicationMode, "en");
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
    public static boolean hasApiAssistantMapping(String apiName, String communicationMode, String lang) {
        String combinedKey = apiName + "_" + communicationMode + "_" + lang;
        return apiAssistantMappingCache.containsKey(combinedKey);
    }
    
    /**
     * Check if API assistant mapping exists with default language - STATIC ACCESS
     */
    public static boolean hasApiAssistantMapping(String apiName, String communicationMode) {
        return hasApiAssistantMapping(apiName, communicationMode, "en");
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
    
    // ==================== LANGUAGE MAPPING STATIC METHODS ====================
    
    /**
     * Get language name from language code - STATIC ACCESS
     * @param languageCode The language code (e.g., "en", "ar", "tr", "hi")
     * @return The language name (e.g., "English", "Arabic", "Turkish", "Hindi") or null if not found
     */
    public static String getLanguageName(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            log.debug("Null or empty language code provided");
            return null;
        }
        return configurationCache.get(languageCode.toLowerCase());
    }
    
    /**
     * Get language name from language code with default fallback - STATIC ACCESS
     * @param languageCode The language code (e.g., "en", "ar", "tr", "hi")
     * @param defaultLanguageName Default language name to return if mapping not found
     * @return The language name or the default value
     */
    public static String getLanguageName(String languageCode, String defaultLanguageName) {
        String languageName = getLanguageName(languageCode);
        if (languageName == null) {
            log.warn("Language name not found for code: '{}', using default: '{}'", 
                    languageCode != null ? languageCode : "null", 
                    defaultLanguageName);
            return defaultLanguageName;
        }
        log.info("Language name for code: {} is {}", languageCode, languageName);
        return languageName;
    }
    
    /**
     * Get default language code from configuration - STATIC ACCESS
     * @return The default language code (usually "en") or "en" as fallback
     */
    public static String getDefaultLanguageCode() {
        return configurationCache.getOrDefault("default_language_id", "en");
    }
    
    /**
     * Get default language name - STATIC ACCESS
     * @return The name of the default language (e.g., "English")
     */
    public static String getDefaultLanguageName() {
        String defaultLangCode = getDefaultLanguageCode();
        return getLanguageName(defaultLangCode, "English");
    }
    
    /**
     * Check if a language mapping exists - STATIC ACCESS
     * @param languageCode The language code to check
     * @return true if the language mapping exists, false otherwise
     */
    public static boolean hasLanguageMapping(String languageCode) {
        return languageCode != null && configurationCache.containsKey(languageCode.toLowerCase());
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