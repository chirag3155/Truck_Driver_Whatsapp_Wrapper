package com.driver.whatsapp.wrapper.controller;

import com.driver.whatsapp.wrapper.service.ConfigurationCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wawrapper/config")
@Slf4j
@Tag(name = "Configuration Management", description = "APIs for managing wrapper configurations")
public class ConfigurationController {

    @Autowired
    private ConfigurationCacheService configService; // Only needed for reload functionality

    /**
     * Get all cached configurations
     */
    @Operation(
        summary = "Get All Configurations",
        description = "Retrieves all cached configurations for the current module"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configurations retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "status": "success",
                      "module": "trukker_wrapper",
                      "count": 5,
                      "configurations": {
                        "api_timeout": "30000",
                        "max_retries": "3",
                        "default_message": "Hello from TruKKer"
                      }
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllConfigurations() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("module", ConfigurationCacheService.getModuleName());
        response.put("wrapperConfigCount", ConfigurationCacheService.getCacheSize());
        response.put("apiAssistantMappingCount", ConfigurationCacheService.getApiAssistantMappingCacheSize());
        response.put("wrapperConfigurations", ConfigurationCacheService.getAllConfigurations());
        response.put("apiAssistantMappings", ConfigurationCacheService.getAllApiAssistantMappings());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific configuration by parameter ID
     */
    @Operation(
        summary = "Get Configuration by Parameter ID",
        description = "Retrieves a specific configuration value by parameter ID"
    )
    @GetMapping("/{paramId}")
    public ResponseEntity<Map<String, Object>> getConfiguration(@PathVariable String paramId) {
        String value = ConfigurationCacheService.getConfigValue(paramId);
        
        Map<String, Object> response = new HashMap<>();
        if (value != null) {
            response.put("status", "success");
            response.put("paramId", paramId);
            response.put("value", value);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "not_found");
            response.put("message", "Configuration not found for parameter: " + paramId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reload configurations from database
     */
    @Operation(
        summary = "Reload Configurations",
        description = "Manually reload configurations from database into cache"
    )
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadConfigurations() {
        try {
            configService.reloadConfigurations();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All configurations reloaded successfully");
            response.put("wrapperConfigCount", ConfigurationCacheService.getCacheSize());
            response.put("apiAssistantMappingCount", ConfigurationCacheService.getApiAssistantMappingCacheSize());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error reloading configurations: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to reload configurations: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get configuration cache statistics
     */
    @Operation(
        summary = "Get Cache Statistics",
        description = "Returns statistics about the configuration cache"
    )
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("module", ConfigurationCacheService.getModuleName());
        response.put("wrapperConfigCacheSize", ConfigurationCacheService.getCacheSize());
        response.put("apiAssistantMappingCacheSize", ConfigurationCacheService.getApiAssistantMappingCacheSize());
        response.put("wrapperConfigKeys", ConfigurationCacheService.getConfigKeys());
        response.put("apiAssistantMappingKeys", ConfigurationCacheService.getApiAssistantMappingKeys());
        response.put("initialized", ConfigurationCacheService.isCacheInitialized());
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific API assistant mapping by API name and communication mode
     */
    @Operation(
        summary = "Get API Assistant Mapping",
        description = "Retrieves API assistant mapping for specific API name and communication mode"
    )
    @GetMapping("/api-mapping/{apiName}/{communicationMode}")
    public ResponseEntity<Map<String, Object>> getApiAssistantMapping(
            @PathVariable String apiName, 
            @PathVariable String communicationMode) {
        
        String tenantId = ConfigurationCacheService.getTenantId(apiName, communicationMode);
        String assistantId = ConfigurationCacheService.getAssistantId(apiName, communicationMode);
        
        Map<String, Object> response = new HashMap<>();
        if (tenantId != null && assistantId != null) {
            response.put("status", "success");
            response.put("apiName", apiName);
            response.put("communicationMode", communicationMode);
            response.put("tenantId", tenantId);
            response.put("assistantId", assistantId);
            response.put("combinedKey", apiName + "_" + communicationMode);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "not_found");
            response.put("message", "API assistant mapping not found for: " + apiName + "_" + communicationMode);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all API assistant mappings only
     */
    @Operation(
        summary = "Get All API Assistant Mappings",
        description = "Retrieves all cached API assistant mappings"
    )
    @GetMapping("/api-mappings")
    public ResponseEntity<Map<String, Object>> getAllApiAssistantMappings() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", ConfigurationCacheService.getApiAssistantMappingCacheSize());
        response.put("mappings", ConfigurationCacheService.getAllApiAssistantMappings());
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
} 