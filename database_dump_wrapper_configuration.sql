-- Database dump for wrapper_configuration table
-- This file contains INSERT statements for all configuration parameters defined in ConfigurationConstants.java
-- Execute this SQL to populate your wrapper_configuration table with default values

-- Clear existing data for this module (optional)
DELETE FROM wrapper_configuration WHERE module_name = 'trukker_wrapper';

-- ==================== API & CONNECTION SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'api_timeout', 'API Timeout (milliseconds)', '30000'),
('trukker_wrapper', 'max_retries', 'Maximum Retry Attempts', '3'),
('trukker_wrapper', 'retry_delay_ms', 'Retry Delay (milliseconds)', '1000'),
('trukker_wrapper', 'connection_timeout', 'Connection Timeout (milliseconds)', '10000'),
('trukker_wrapper', 'read_timeout', 'Read Timeout (milliseconds)', '30000');

-- ==================== WHATSAPP TEMPLATE SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'whatsapp_template_name', 'WhatsApp Template Name', '11_start_time_due'),
('trukker_wrapper', 'template_language', 'Template Language Code', 'en'),
('trukker_wrapper', 'fallback_template_name', 'Fallback Template Name', 'simple_text_template');

-- ==================== BUTTON TEXT CONFIGURATIONS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'yes_button_text', 'Yes Button Text', 'Yes I am on time'),
('trukker_wrapper', 'no_button_text', 'No Button Text', 'I am late'),
('trukker_wrapper', 'confirm_button_text', 'Confirm Button Text', 'Confirm'),
('trukker_wrapper', 'cancel_button_text', 'Cancel Button Text', 'Cancel');

-- ==================== MESSAGE TEMPLATES ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'welcome_message_template', 'Welcome Message Template', 'Hello {driver_name}, welcome to TruKKer! We will keep you updated on your delivery progress.'),
('trukker_wrapper', 'new_eta_message', 'New ETA Request Message', 'We understand you''re facing a delay. Please provide your new estimated arrival time (e.g., 10:30 AM).'),
('trukker_wrapper', 'breakdown_inquiry_template', 'Breakdown Inquiry Template', 'We understand you''re facing an issue. Please let us know:\n1. Vehicle breakdown\n2. Traffic delay\n3. Other (please specify)'),
('trukker_wrapper', 'confirmation_message_template', 'Confirmation Message Template', 'Thank you for the update. We have recorded your information and will keep the customer informed.'),
('trukker_wrapper', 'delay_notification_template', 'Delay Notification Template', 'We have been notified of a delay in your delivery. Please provide an updated ETA when possible.'),
('trukker_wrapper', 'arrival_confirmation_template', 'Arrival Confirmation Template', 'Please confirm when you have arrived at the destination. Reply with "ARRIVED" when you reach the delivery location.'),
('trukker_wrapper', 'error_message_template', 'Error Message Template', 'Sorry, I didn''t understand your response. Please reply with one of the provided options or contact support.');

-- ==================== CALLBACK DATA CONFIGURATIONS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'initial_callback_data', 'Initial Callback Data', 'initial_eta_check'),
('trukker_wrapper', 'default_callback_data', 'Default Callback Data', 'text_message'),
('trukker_wrapper', 'eta_update_callback_data', 'ETA Update Callback Data', 'eta_update'),
('trukker_wrapper', 'delay_callback_data', 'Delay Callback Data', 'delay_notification'),
('trukker_wrapper', 'arrival_callback_data', 'Arrival Callback Data', 'arrival_confirmation');

-- ==================== FEATURE FLAGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'enable_notifications', 'Enable Notifications', 'true'),
('trukker_wrapper', 'enable_failure_alerts', 'Enable Failure Alerts', 'true'),
('trukker_wrapper', 'enable_auto_retry', 'Enable Auto Retry', 'true'),
('trukker_wrapper', 'enable_detailed_logging', 'Enable Detailed Logging', 'false'),
('trukker_wrapper', 'enable_webhook_notifications', 'Enable Webhook Notifications', 'true'),
('trukker_wrapper', 'enable_sms_fallback', 'Enable SMS Fallback', 'false');

-- ==================== NOTIFICATION SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'notification_template', 'Notification Template', 'ALERT: {message} - Time: {timestamp} - Driver: {driver_phone}'),
('trukker_wrapper', 'notification_emails', 'Notification Email Addresses', 'ops@trukker.com,alerts@trukker.com'),
('trukker_wrapper', 'notification_webhook_url', 'Notification Webhook URL', 'https://api.trukker.com/webhooks/alerts');

-- ==================== MESSAGE FORMATTING ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'confirmation_prefix', 'Confirmation Message Prefix', '✅ '),
('trukker_wrapper', 'error_prefix', 'Error Message Prefix', '❌ '),
('trukker_wrapper', 'warning_prefix', 'Warning Message Prefix', '⚠️ '),
('trukker_wrapper', 'info_prefix', 'Info Message Prefix', 'ℹ️ ');

-- ==================== TIMING CONFIGURATIONS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'max_response_wait_time', 'Max Response Wait Time (minutes)', '30'),
('trukker_wrapper', 'auto_followup_delay', 'Auto Followup Delay (minutes)', '15'),
('trukker_wrapper', 'eta_tolerance_minutes', 'ETA Tolerance (minutes)', '15'),
('trukker_wrapper', 'session_timeout_minutes', 'Session Timeout (minutes)', '60');

-- ==================== BUSINESS LOGIC SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'max_allowed_delay_minutes', 'Max Allowed Delay (minutes)', '120'),
('trukker_wrapper', 'min_eta_update_interval', 'Min ETA Update Interval (minutes)', '5'),
('trukker_wrapper', 'auto_escalation_threshold', 'Auto Escalation Threshold (minutes)', '60');

-- ==================== INTEGRATION SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'trukker_api_timeout', 'TruKKer API Timeout (milliseconds)', '15000'),
('trukker_wrapper', 'external_webhook_timeout', 'External Webhook Timeout (milliseconds)', '10000'),
('trukker_wrapper', 'chat_module_timeout', 'Chat Module Timeout (milliseconds)', '20000');

-- ==================== VALIDATION SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'phone_validation_pattern', 'Phone Number Validation Pattern', '^[0-9]{12}$'),
('trukker_wrapper', 'order_id_validation_pattern', 'Order ID Validation Pattern', '^ORD[0-9]{9}$'),
('trukker_wrapper', 'driver_name_validation_pattern', 'Driver Name Validation Pattern', '^[a-zA-Z\\s]{2,50}$');

-- ==================== CACHE AND PERFORMANCE ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'cache_refresh_interval', 'Cache Refresh Interval (minutes)', '60'),
('trukker_wrapper', 'max_cache_size', 'Maximum Cache Size', '1000'),
('trukker_wrapper', 'enable_cache_stats', 'Enable Cache Statistics', 'false');

-- ==================== ADDITIONAL WHATSAPP SPECIFIC SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'whatsapp_message_limit', 'WhatsApp Daily Message Limit', '1000'),
('trukker_wrapper', 'whatsapp_rate_limit_per_minute', 'WhatsApp Rate Limit Per Minute', '100'),
('trukker_wrapper', 'enable_delivery_reports', 'Enable WhatsApp Delivery Reports', 'true'),
('trukker_wrapper', 'delivery_report_webhook', 'Delivery Report Webhook URL', '/wawrapper/whatsapp/delivery-report');

-- ==================== DRIVER INTERACTION SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'driver_response_timeout', 'Driver Response Timeout (minutes)', '45'),
('trukker_wrapper', 'max_followup_attempts', 'Maximum Followup Attempts', '3'),
('trukker_wrapper', 'escalation_after_attempts', 'Escalate After Attempts', '2'),
('trukker_wrapper', 'enable_auto_escalation', 'Enable Auto Escalation', 'true');

-- ==================== OPERATIONAL SETTINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'working_hours_start', 'Working Hours Start (24hr format)', '06:00'),
('trukker_wrapper', 'working_hours_end', 'Working Hours End (24hr format)', '22:00'),
('trukker_wrapper', 'weekend_support_enabled', 'Weekend Support Enabled', 'true'),
('trukker_wrapper', 'emergency_contact_enabled', 'Emergency Contact Enabled', 'true'),
('trukker_wrapper', 'emergency_contact_number', 'Emergency Contact Number', '+971-800-TRUKKER');

-- ==================== LOGGING AND MONITORING ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'log_level', 'Application Log Level', 'INFO'),
('trukker_wrapper', 'enable_performance_monitoring', 'Enable Performance Monitoring', 'true'),
('trukker_wrapper', 'enable_error_tracking', 'Enable Error Tracking', 'true'),
('trukker_wrapper', 'monitoring_webhook_url', 'Monitoring Webhook URL', 'https://monitoring.trukker.com/webhooks/alerts');

-- Verify the inserted data
SELECT COUNT(*) as total_configs FROM wrapper_configuration WHERE module_name = 'trukker_wrapper';

-- Display all configurations for verification
SELECT param_id, param_name, param_value 
FROM wrapper_configuration 
WHERE module_name = 'trukker_wrapper' 
ORDER BY param_id; 