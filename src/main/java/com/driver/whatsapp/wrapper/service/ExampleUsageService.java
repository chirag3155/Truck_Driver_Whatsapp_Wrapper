package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.constants.ConfigurationConstants;
import com.driver.whatsapp.wrapper.entity.ApiAssistantMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Example service demonstrating how to use the static ConfigurationCacheService directly
 * throughout the application without dependency injection or utility classes
 */
@Service
@Slf4j
public class ExampleUsageService {

    /**
     * Example method showing how to use static configuration cache with constants
     */
    public void processDriverMessage() {
        // Get API timeout from configuration using constants (with default fallback)
        String apiTimeoutStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000");
        
        // Get max retries configuration using constants
        String maxRetriesStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.MAX_RETRIES, "3");
        
        // Check if notifications feature is enabled
        if (ConfigurationCacheService.hasConfig(ConfigurationConstants.ENABLE_NOTIFICATIONS)) {
            String notificationTemplate = ConfigurationCacheService.getConfigValue(ConfigurationConstants.NOTIFICATION_TEMPLATE, "Default message");
            log.info("Notifications enabled with template: {}", notificationTemplate);
        }
        
        // Convert to proper types with error handling
        int apiTimeout = parseIntSafely(apiTimeoutStr, 30000);
        int maxRetries = parseIntSafely(maxRetriesStr, 3);
        
        // Use the configuration values
        log.info("Processing driver message with timeout: {}ms, max retries: {}", apiTimeout, maxRetries);
        
        // Your business logic here...
        processWithConfiguration(apiTimeout, maxRetries);
    }
    
    /**
     * Example showing API assistant mapping usage
     */
    public void handleDriverDetailsAPI(String communicationMode) {
        // Get tenant and assistant for driver details API
        String tenantId = ConfigurationCacheService.getTenantId("driver_details", communicationMode);
        String assistantId = ConfigurationCacheService.getAssistantId("driver_details", communicationMode);
        
        if (tenantId != null && assistantId != null) {
            log.info("Processing driver_details with tenant: {}, assistant: {}, mode: {}", 
                    tenantId, assistantId, communicationMode);
            
            // Get the complete mapping if needed
            ApiAssistantMapping mapping = ConfigurationCacheService.getApiAssistantMapping("driver_details", communicationMode);
            if (mapping != null) {
                log.info("Full mapping: {}", mapping.getCombinedKey());
            }
        } else {
            log.warn("No mapping found for driver_details with communication mode: {}", communicationMode);
        }
    }
    
    /**
     * Example showing WhatsApp specific configuration usage
     */
    public void sendWhatsAppMessage(String driverPhone, String driverName) {
        // Get WhatsApp template configuration
        String templateName = ConfigurationCacheService.getConfigValue(ConfigurationConstants.WHATSAPP_TEMPLATE_NAME, "11_start_time_due");
        String templateLanguage = ConfigurationCacheService.getConfigValue(ConfigurationConstants.TEMPLATE_LANGUAGE, "en");
        
        // Get button text configurations
        String yesButtonText = ConfigurationCacheService.getConfigValue(ConfigurationConstants.YES_BUTTON_TEXT, "Yes I am on time");
        String noButtonText = ConfigurationCacheService.getConfigValue(ConfigurationConstants.NO_BUTTON_TEXT, "I am late");
        
        // Get message template and format it
        String welcomeTemplate = ConfigurationCacheService.getConfigValue(ConfigurationConstants.WELCOME_MESSAGE_TEMPLATE, 
                "Hello {driver_name}, welcome to TruKKer!");
        String welcomeMessage = welcomeTemplate.replace("{driver_name}", driverName);
        
        // Get API assistant mapping for WhatsApp
        String tenantId = ConfigurationCacheService.getTenantId("driver_details", "whatsapp");
        String assistantId = ConfigurationCacheService.getAssistantId("driver_details", "whatsapp");
        
        log.info("Sending WhatsApp message to {} using template: {}, language: {}, tenant: {}, assistant: {}", 
                driverPhone, templateName, templateLanguage, tenantId, assistantId);
        
        // Your WhatsApp sending logic here...
    }
    
    /**
     * Example showing feature flags and message formatting
     */
    public String formatConfirmationMessage(String message) {
        // Check if detailed logging is enabled
        boolean detailedLogging = isFeatureEnabled(ConfigurationConstants.ENABLE_DETAILED_LOGGING);
        
        if (detailedLogging) {
            log.debug("Formatting confirmation message: {}", message);
        }
        
        // Get confirmation prefix from configuration
        String confirmationPrefix = ConfigurationCacheService.getConfigValue(ConfigurationConstants.CONFIRMATION_PREFIX, "✅ ");
        
        // Format the message
        String formattedMessage = confirmationPrefix + message;
        
        if (detailedLogging) {
            log.debug("Formatted message: {}", formattedMessage);
        }
        
        return formattedMessage;
    }
    
    /**
     * Example showing retry logic with configuration
     */
    public void performOperationWithRetry() {
        // Get retry configuration
        String maxRetriesStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.MAX_RETRIES, "3");
        String retryDelayStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.RETRY_DELAY_MS, "1000");
        
        int maxRetries = parseIntSafely(maxRetriesStr, 3);
        int retryDelay = parseIntSafely(retryDelayStr, 1000);
        
        // Check if auto retry is enabled
        if (isFeatureEnabled(ConfigurationConstants.ENABLE_AUTO_RETRY)) {
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    log.info("Attempting operation, attempt {}/{}", attempt, maxRetries);
                    
                    // Your operation logic here
                    performActualOperation();
                    
                    log.info("Operation succeeded on attempt {}", attempt);
                    break; // Success - exit retry loop
                    
                } catch (Exception e) {
                    log.warn("Operation failed on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                    
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.error("All {} retry attempts failed", maxRetries);
                        // Handle final failure
                    }
                }
            }
        } else {
            log.info("Auto retry is disabled, performing single attempt");
            performActualOperation();
        }
    }
    
    /**
     * Example showing timeout configuration usage
     */
    public void callExternalAPI() {
        // Get timeout configurations
        String connectionTimeoutStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.CONNECTION_TIMEOUT, "10000");
        String readTimeoutStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.READ_TIMEOUT, "30000");
        String truKKerApiTimeoutStr = ConfigurationCacheService.getConfigValue(ConfigurationConstants.TRUKKER_API_TIMEOUT, "15000");
        
        int connectionTimeout = parseIntSafely(connectionTimeoutStr, 10000);
        int readTimeout = parseIntSafely(readTimeoutStr, 30000);
        int apiTimeout = parseIntSafely(truKKerApiTimeoutStr, 15000);
        
        log.info("Calling external API with timeouts - connection: {}ms, read: {}ms, api: {}ms", 
                connectionTimeout, readTimeout, apiTimeout);
        
        // Your API call logic with these timeouts...
    }
    
    /**
     * Example showing validation configuration usage
     */
    public boolean validateDriverData(String driverName, String phoneNumber, String orderId) {
        // Get validation patterns from configuration
        String phonePattern = ConfigurationCacheService.getConfigValue(ConfigurationConstants.PHONE_VALIDATION_PATTERN, "^[0-9]{12}$");
        String driverNamePattern = ConfigurationCacheService.getConfigValue(ConfigurationConstants.DRIVER_NAME_VALIDATION_PATTERN, "^[a-zA-Z\\s]{2,50}$");
        String orderIdPattern = ConfigurationCacheService.getConfigValue(ConfigurationConstants.ORDER_ID_VALIDATION_PATTERN, "^ORD[0-9]{9}$");
        
        // Perform validation
        boolean phoneValid = phoneNumber.matches(phonePattern);
        boolean nameValid = driverName.matches(driverNamePattern);
        boolean orderValid = orderId.matches(orderIdPattern);
        
        if (!phoneValid) {
            log.warn("Invalid phone number format: {}", phoneNumber);
        }
        if (!nameValid) {
            log.warn("Invalid driver name format: {}", driverName);
        }
        if (!orderValid) {
            log.warn("Invalid order ID format: {}", orderId);
        }
        
        return phoneValid && nameValid && orderValid;
    }
    
    /**
     * Example showing how to check multiple communication modes for an API
     */
    public void checkAPISupport(String apiName) {
        log.info("Checking communication mode support for API: {}", apiName);
        
        // Check different communication modes
        String[] modes = {"whatsapp", "sms", "email", "chat", "voice"};
        
        for (String mode : modes) {
            if (ConfigurationCacheService.hasApiAssistantMapping(apiName, mode)) {
                String tenantId = ConfigurationCacheService.getTenantId(apiName, mode);
                String assistantId = ConfigurationCacheService.getAssistantId(apiName, mode);
                String nextMode = ConfigurationCacheService.getNextCommunicationMode(apiName, mode);
                log.info("  {} - ✅ supported (tenant: {}, assistant: {}, next: {})", 
                        mode, tenantId, assistantId, nextMode);
            } else {
                log.info("  {} - ❌ not supported", mode);
            }
        }
    }
    
    /**
     * Example showing how to use nextCommunicationMode for fallback scenarios
     */
    public void handleCommunicationWithFallback(String apiName, String initialMode, String driverPhone, String message) {
        log.info("Attempting communication for API: {} starting with mode: {}", apiName, initialMode);
        
        String currentMode = initialMode;
        int maxFallbackAttempts = 3; // Prevent infinite loops
        int attempt = 1;
        
        while (currentMode != null && attempt <= maxFallbackAttempts) {
            // Check if current mode is supported
            if (ConfigurationCacheService.hasApiAssistantMapping(apiName, currentMode)) {
                String tenantId = ConfigurationCacheService.getTenantId(apiName, currentMode);
                String assistantId = ConfigurationCacheService.getAssistantId(apiName, currentMode);
                String nextMode = ConfigurationCacheService.getNextCommunicationMode(apiName, currentMode);
                
                log.info("Attempt {}: Trying {} (tenant: {}, assistant: {}, next fallback: {})", 
                        attempt, currentMode, tenantId, assistantId, nextMode);
                
                // Simulate sending message with current mode
                boolean success = attemptSendMessage(currentMode, driverPhone, message, tenantId, assistantId);
                
                if (success) {
                    log.info("✅ Message sent successfully via {}", currentMode);
                    return; // Success - exit
                } else {
                    log.warn("❌ Failed to send via {}, trying fallback: {}", currentMode, nextMode);
                    currentMode = nextMode; // Move to next communication mode
                    attempt++;
                }
            } else {
                log.error("No mapping found for API: {} with mode: {}", apiName, currentMode);
                break;
            }
        }
        
        // If we reach here, all attempts failed
        log.error("🚨 CRITICAL: All communication attempts failed for API: {} starting from mode: {}", 
                apiName, initialMode);
        // TODO: Trigger alert to operations team, store for manual follow-up, etc.
    }
    
    /**
     * Example showing intelligent communication mode selection based on driver preferences
     */
    public void sendDriverNotificationWithPreference(String driverPhone, String message, String preferredMode) {
        log.info("Sending notification to driver {} with preferred mode: {}", driverPhone, preferredMode);
        
        // Try preferred mode first for driver_details API
        boolean success = false;
        if (ConfigurationCacheService.hasApiAssistantMapping("driver_details", preferredMode)) {
            String tenantId = ConfigurationCacheService.getTenantId("driver_details", preferredMode);
            String assistantId = ConfigurationCacheService.getAssistantId("driver_details", preferredMode);
            
            log.info("Attempting preferred mode: {} (tenant: {}, assistant: {})", 
                    preferredMode, tenantId, assistantId);
            success = attemptSendMessage(preferredMode, driverPhone, message, tenantId, assistantId);
        }
        
        // If preferred mode failed, use the fallback chain
        if (!success) {
            log.warn("Preferred mode {} failed, initiating fallback chain", preferredMode);
            handleCommunicationWithFallback("driver_details", preferredMode, driverPhone, message);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Helper method to check if a feature is enabled
     */
    private boolean isFeatureEnabled(String featureKey) {
        String value = ConfigurationCacheService.getConfigValue(featureKey, "false");
        return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
    }
    
    /**
     * Helper method to safely parse integers with fallback
     */
    private int parseIntSafely(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value: {}, using default: {}", value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Dummy method for demonstration
     */
    private void performActualOperation() {
        // Your actual operation logic
        log.info("Performing actual operation...");
    }
    
    /**
     * Dummy method for demonstration
     */
    private void processWithConfiguration(int timeout, int retries) {
        // Your processing logic with configuration
        log.info("Processing with configured timeout: {}ms and retries: {}", timeout, retries);
    }
    
    /**
     * Helper method to simulate sending a message via different communication modes
     * In a real implementation, this would call the appropriate service (WhatsApp, SMS, etc.)
     */
    private boolean attemptSendMessage(String mode, String phone, String message, String tenantId, String assistantId) {
        try {
            log.info("Attempting to send message via {} to {} using tenant: {}, assistant: {}", 
                    mode, phone, tenantId, assistantId);
            
            // Simulate different success rates for different modes (for demonstration)
            switch (mode.toLowerCase()) {
                case "whatsapp":
                    // WhatsApp has 80% success rate in this simulation
                    return Math.random() > 0.2;
                case "sms":
                    // SMS has 90% success rate
                    return Math.random() > 0.1;
                case "email":
                    // Email has 95% success rate
                    return Math.random() > 0.05;
                case "chat":
                    // Chat has 70% success rate
                    return Math.random() > 0.3;
                case "voice":
                    // Voice has 60% success rate
                    return Math.random() > 0.4;
                default:
                    log.warn("Unknown communication mode: {}", mode);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error sending message via {}: {}", mode, e.getMessage());
            return false;
        }
    }
} 