package com.driver.whatsapp.wrapper.constants;

/**
 * Constants class containing all configuration parameter IDs used throughout the application.
 * This ensures consistency and prevents typos when referencing configuration parameters.
 * 
 * Usage: ConfigurationCacheService.getConfigValue(ConfigurationConstants.API_TIMEOUT, "30000")
 */
public final class ConfigurationConstants {

    // Private constructor to prevent instantiation
    private ConfigurationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== API & CONNECTION SETTINGS ====================
    
    /**
     * API timeout in milliseconds (default: 30000)
     */
    public static final String API_TIMEOUT = "api_timeout";
    
    /**
     * Maximum number of retry attempts (default: 3)
     */
    public static final String MAX_RETRIES = "max_retries";
    
    /**
     * Retry delay in milliseconds between attempts (default: 1000)
     */
    public static final String RETRY_DELAY_MS = "retry_delay_ms";
    
    /**
     * Connection timeout in milliseconds (default: 10000)
     */
    public static final String CONNECTION_TIMEOUT = "connection_timeout";
    
    /**
     * Read timeout in milliseconds (default: 30000)
     */
    public static final String READ_TIMEOUT = "read_timeout";

    // ==================== WHATSAPP TEMPLATE SETTINGS ====================
    
    /**
     * WhatsApp template name for initial driver message (default: "11_start_time_due")
     */
    public static final String WHATSAPP_TEMPLATE_NAME = "whatsapp_template_name";
    
    /**
     * Template language code (default: "en")
     */
    public static final String TEMPLATE_LANGUAGE = "template_language";
    
    /**
     * Alternative template name for fallback scenarios
     */
    public static final String FALLBACK_TEMPLATE_NAME = "fallback_template_name";

    // ==================== BUTTON TEXT CONFIGURATIONS ====================
    
    /**
     * Text for "Yes" button in templates (default: "Yes I am on time")
     */
    public static final String YES_BUTTON_TEXT = "yes_button_text";
    
    /**
     * Text for "No" button in templates (default: "I am late")
     */
    public static final String NO_BUTTON_TEXT = "no_button_text";
    
    /**
     * Text for "Confirm" button (default: "Confirm")
     */
    public static final String CONFIRM_BUTTON_TEXT = "confirm_button_text";
    
    /**
     * Text for "Cancel" button (default: "Cancel")
     */
    public static final String CANCEL_BUTTON_TEXT = "cancel_button_text";

    // ==================== MESSAGE TEMPLATES ====================
    
    /**
     * Welcome message template with placeholder {driver_name}
     */
    public static final String WELCOME_MESSAGE_TEMPLATE = "welcome_message_template";
    
    /**
     * New ETA request message template
     */
    public static final String NEW_ETA_MESSAGE = "new_eta_message";
    
    /**
     * Breakdown inquiry message template
     */
    public static final String BREAKDOWN_INQUIRY_TEMPLATE = "breakdown_inquiry_template";
    
    /**
     * Confirmation message template
     */
    public static final String CONFIRMATION_MESSAGE_TEMPLATE = "confirmation_message_template";
    
    /**
     * Delay notification message template
     */
    public static final String DELAY_NOTIFICATION_TEMPLATE = "delay_notification_template";
    
    /**
     * Arrival confirmation message template
     */
    public static final String ARRIVAL_CONFIRMATION_TEMPLATE = "arrival_confirmation_template";
    
    /**
     * Error message template for invalid responses
     */
    public static final String ERROR_MESSAGE_TEMPLATE = "error_message_template";

    // ==================== CALLBACK DATA CONFIGURATIONS ====================
    
    /**
     * Initial callback data for ETA check (default: "initial_eta_check")
     */
    public static final String INITIAL_CALLBACK_DATA = "initial_callback_data";
    
    /**
     * Default callback data for text messages (default: "text_message")
     */
    public static final String DEFAULT_CALLBACK_DATA = "default_callback_data";
    
    /**
     * Callback data for ETA updates
     */
    public static final String ETA_UPDATE_CALLBACK_DATA = "eta_update_callback_data";
    
    /**
     * Callback data for delay notifications
     */
    public static final String DELAY_CALLBACK_DATA = "delay_callback_data";
    
    /**
     * Callback data for arrival confirmations
     */
    public static final String ARRIVAL_CALLBACK_DATA = "arrival_callback_data";

    // ==================== FEATURE FLAGS ====================
    
    /**
     * Enable/disable notifications feature (default: true)
     */
    public static final String ENABLE_NOTIFICATIONS = "enable_notifications";
    
    /**
     * Enable/disable failure alerts (default: true)
     */
    public static final String ENABLE_FAILURE_ALERTS = "enable_failure_alerts";
    
    /**
     * Enable/disable automatic retries (default: true)
     */
    public static final String ENABLE_AUTO_RETRY = "enable_auto_retry";
    
    /**
     * Enable/disable detailed logging (default: false)
     */
    public static final String ENABLE_DETAILED_LOGGING = "enable_detailed_logging";
    
    /**
     * Enable/disable webhook notifications (default: true)
     */
    public static final String ENABLE_WEBHOOK_NOTIFICATIONS = "enable_webhook_notifications";
    
    /**
     * Enable/disable SMS fallback (default: false)
     */
    public static final String ENABLE_SMS_FALLBACK = "enable_sms_fallback";

    // ==================== NOTIFICATION SETTINGS ====================
    
    /**
     * Notification template for alerts
     */
    public static final String NOTIFICATION_TEMPLATE = "notification_template";
    
    /**
     * Notification email addresses (comma-separated)
     */
    public static final String NOTIFICATION_EMAILS = "notification_emails";
    
    /**
     * Notification webhook URL
     */
    public static final String NOTIFICATION_WEBHOOK_URL = "notification_webhook_url";

    // ==================== MESSAGE FORMATTING ====================
    
    /**
     * Prefix for confirmation messages (default: "✅ ")
     */
    public static final String CONFIRMATION_PREFIX = "confirmation_prefix";
    
    /**
     * Prefix for error messages (default: "❌ ")
     */
    public static final String ERROR_PREFIX = "error_prefix";
    
    /**
     * Prefix for warning messages (default: "⚠️ ")
     */
    public static final String WARNING_PREFIX = "warning_prefix";
    
    /**
     * Prefix for info messages (default: "ℹ️ ")
     */
    public static final String INFO_PREFIX = "info_prefix";

    // ==================== TIMING CONFIGURATIONS ====================
    
    /**
     * Maximum wait time for driver response in minutes (default: 30)
     */
    public static final String MAX_RESPONSE_WAIT_TIME = "max_response_wait_time";
    
    /**
     * Auto-followup delay in minutes (default: 15)
     */
    public static final String AUTO_FOLLOWUP_DELAY = "auto_followup_delay";
    
    /**
     * ETA tolerance in minutes (default: 15)
     */
    public static final String ETA_TOLERANCE_MINUTES = "eta_tolerance_minutes";
    
    /**
     * Session timeout in minutes (default: 60)
     */
    public static final String SESSION_TIMEOUT_MINUTES = "session_timeout_minutes";

    // ==================== BUSINESS LOGIC SETTINGS ====================
    
    /**
     * Maximum allowed delay in minutes (default: 120)
     */
    public static final String MAX_ALLOWED_DELAY_MINUTES = "max_allowed_delay_minutes";
    
    /**
     * Minimum ETA update interval in minutes (default: 5)
     */
    public static final String MIN_ETA_UPDATE_INTERVAL = "min_eta_update_interval";
    
    /**
     * Auto-escalation threshold in minutes (default: 60)
     */
    public static final String AUTO_ESCALATION_THRESHOLD = "auto_escalation_threshold";

    // ==================== INTEGRATION SETTINGS ====================
    
    /**
     * TruKKer API timeout in milliseconds (default: 15000)
     */
    public static final String TRUKKER_API_TIMEOUT = "trukker_api_timeout";
    
    /**
     * External webhook timeout in milliseconds (default: 10000)
     */
    public static final String EXTERNAL_WEBHOOK_TIMEOUT = "external_webhook_timeout";
    
    /**
     * Chat module integration timeout (default: 20000)
     */
    public static final String CHAT_MODULE_TIMEOUT = "chat_module_timeout";

    // ==================== VALIDATION SETTINGS ====================
    
    /**
     * Phone number validation pattern
     */
    public static final String PHONE_VALIDATION_PATTERN = "phone_validation_pattern";
    
    /**
     * Order ID validation pattern
     */
    public static final String ORDER_ID_VALIDATION_PATTERN = "order_id_validation_pattern";
    
    /**
     * Driver name validation pattern
     */
    public static final String DRIVER_NAME_VALIDATION_PATTERN = "driver_name_validation_pattern";

    // ==================== CACHE AND PERFORMANCE ====================
    
    /**
     * Configuration cache refresh interval in minutes (default: 60)
     */
    public static final String CACHE_REFRESH_INTERVAL = "cache_refresh_interval";
    
    /**
     * Maximum cache size (default: 1000)
     */
    public static final String MAX_CACHE_SIZE = "max_cache_size";
    
    /**
     * Enable cache statistics (default: false)
     */
    public static final String ENABLE_CACHE_STATS = "enable_cache_stats";
} 