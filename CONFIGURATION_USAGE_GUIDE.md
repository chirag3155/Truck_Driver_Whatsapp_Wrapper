# Configuration Usage Guide - Direct Service Access

## Overview
Use `ConfigurationCacheService` static methods directly throughout your application without utility classes.

## Basic Usage Patterns

### 1. Wrapper Configuration Access
```java
import com.driver.whatsapp.wrapper.constants.ConfigurationConstants;
import com.driver.whatsapp.wrapper.service.ConfigurationCacheService;

// Get configuration values
String apiTimeout = ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000");
String maxRetries = ConfigurationCacheService.getConfigValue(ConfigurationConstants.MAX_RETRIES, "3");

// Check if configuration exists
if (ConfigurationCacheService.hasConfig(ConfigurationConstants.ENABLE_NOTIFICATIONS)) {
    // Feature is configured
}
```

### 2. API Assistant Mapping Access
```java
// Get tenant and assistant
String tenantId = ConfigurationCacheService.getTenantId("driver_details", "whatsapp");
String assistantId = ConfigurationCacheService.getAssistantId("driver_details", "whatsapp");

// Check if mapping exists
if (ConfigurationCacheService.hasApiAssistantMapping("driver_details", "sms")) {
    // SMS mapping available
}
```

## Usage in Controllers
```java
@PostMapping("/driver-details")
public ResponseEntity<String> receiveDriverDetails(@RequestBody DriverDetails details) {
    
    // Get timeout configuration
    String timeoutStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000");
    int timeout = Integer.parseInt(timeoutStr);
    
    // Get WhatsApp mapping
    String tenantId = ConfigurationCacheService.getTenantId("driver_details", "whatsapp");
    String assistantId = ConfigurationCacheService.getAssistantId("driver_details", "whatsapp");
    
    log.info("Processing with tenant: {}, assistant: {}", tenantId, assistantId);
    
    return ResponseEntity.ok("Success");
}
```

## Feature Flag Pattern
```java
private boolean isFeatureEnabled(String featureConstant) {
    String value = ConfigurationCacheService.getConfigValue(featureConstant, "false");
    return "true".equalsIgnoreCase(value);
}

// Usage
if (isFeatureEnabled(ConfigurationConstants.ENABLE_NOTIFICATIONS)) {
    // Send notification
}
```

## Safe Parsing Pattern
```java
private int parseIntSafely(String value, int defaultValue) {
    try {
        return Integer.parseInt(value);
    } catch (NumberFormatException e) {
        log.warn("Invalid value: {}, using default: {}", value, defaultValue);
        return defaultValue;
    }
}

// Usage
int timeout = parseIntSafely(
    ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000"), 
    30000
);
```

## Available Static Methods

### Wrapper Configuration
- `getConfigValue(paramId, defaultValue)`
- `hasConfig(paramId)`
- `getCacheSize()`
- `getAllConfigurations()`

### API Assistant Mapping
- `getTenantId(apiName, communicationMode)`
- `getAssistantId(apiName, communicationMode)`
- `hasApiAssistantMapping(apiName, communicationMode)`
- `getAllApiAssistantMappings()`

## Reload Configuration
```bash
curl -X POST http://localhost:8025/wawrapper/config/reload
```

## 🎨 **Best Practices**

### **1. Always Use Constants**
```java
// ❌ Bad - hardcoded strings
String timeout = ConfigurationCacheService.getConfigValue("api_timeout", "30000");

// ✅ Good - use constants
String timeout = ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000");
```

### **2. Always Provide Defaults**
```java
// ❌ Bad - no default
String template = ConfigurationCacheService.getConfigValue(ConfigurationConstants.WHATSAPP_TEMPLATE_NAME);

// ✅ Good - with default
String template = ConfigurationCacheService.getConfigValue(ConfigurationConstants.WHATSAPP_TEMPLATE_NAME, "11_start_time_due");
```

### **3. Handle Type Conversion Safely**
```java
// ❌ Bad - can throw exception
int timeout = Integer.parseInt(ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000"));

// ✅ Good - safe parsing
int timeout = parseIntSafely(
    ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000"), 
    30000
);
```

### **4. Log Configuration Usage**
```java
// ✅ Good - log for debugging
String template = ConfigurationCacheService.getConfigValue(ConfigurationConstants.WHATSAPP_TEMPLATE_NAME, "11_start_time_due");
log.debug("Using WhatsApp template: {}", template);
```

### **5. Check Cache Initialization in Critical Paths**
```java
// ✅ Good - defensive programming
if (!ConfigurationCacheService.isCacheInitialized()) {
    log.warn("Configuration cache not initialized, using defaults");
    // Use hardcoded defaults
    return;
}
```

## 📊 **Monitoring and Debugging**

```java
// Check cache status
log.info("Wrapper config cache size: {}", ConfigurationCacheService.getCacheSize());
log.info("API mapping cache size: {}", ConfigurationCacheService.getApiAssistantMappingCacheSize());
log.info("Cache initialized: {}", ConfigurationCacheService.isCacheInitialized());

// List all configuration keys
ConfigurationCacheService.getConfigKeys().forEach(key -> 
    log.debug("Config key: {}", key));

// List all API mapping keys  
ConfigurationCacheService.getApiAssistantMappingKeys().forEach(key -> 
    log.debug("API mapping key: {}", key));
```

This approach provides direct, type-safe access to both configuration types throughout your application without additional utility layers. 