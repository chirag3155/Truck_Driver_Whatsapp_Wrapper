-- Database dump for api_assistant_mapping table
-- This file contains CREATE TABLE statement and sample INSERT statements for API assistant mappings

-- Create the api_assistant_mapping table
CREATE TABLE IF NOT EXISTS api_assistant_mapping (
    api_name VARCHAR(100) NOT NULL,
    communication_mode VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    assistant_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (api_name, communication_mode)
);

-- Clear existing data (optional)
DELETE FROM api_assistant_mapping;

-- ==================== SAMPLE API ASSISTANT MAPPINGS ====================

-- WhatsApp Communication Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('driver_details', 'whatsapp', 'trukker_tenant', 'whatsapp_driver_assistant'),
('driver_callback', 'whatsapp', 'trukker_tenant', 'whatsapp_driver_assistant'),
('eta_update', 'whatsapp', 'trukker_tenant', 'whatsapp_eta_assistant'),
('delivery_confirmation', 'whatsapp', 'trukker_tenant', 'whatsapp_delivery_assistant'),
('driver_support', 'whatsapp', 'trukker_tenant', 'whatsapp_support_assistant');

-- SMS Communication Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('driver_details', 'sms', 'trukker_tenant', 'sms_driver_assistant'),
('eta_update', 'sms', 'trukker_tenant', 'sms_eta_assistant'),
('emergency_alert', 'sms', 'trukker_tenant', 'sms_emergency_assistant'),
('delivery_confirmation', 'sms', 'trukker_tenant', 'sms_delivery_assistant');

-- Email Communication Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('driver_report', 'email', 'trukker_tenant', 'email_report_assistant'),
('daily_summary', 'email', 'trukker_tenant', 'email_summary_assistant'),
('incident_report', 'email', 'trukker_tenant', 'email_incident_assistant');

-- Chat/Web Communication Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('driver_details', 'chat', 'trukker_tenant', 'chat_driver_assistant'),
('customer_support', 'chat', 'trukker_tenant', 'chat_support_assistant'),
('live_tracking', 'chat', 'trukker_tenant', 'chat_tracking_assistant'),
('feedback_collection', 'chat', 'trukker_tenant', 'chat_feedback_assistant');

-- Voice Communication Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('driver_details', 'voice', 'trukker_tenant', 'voice_driver_assistant'),
('emergency_call', 'voice', 'trukker_tenant', 'voice_emergency_assistant'),
('dispatcher_call', 'voice', 'trukker_tenant', 'voice_dispatcher_assistant');

-- Push Notification Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('driver_notification', 'push', 'trukker_tenant', 'push_driver_assistant'),
('order_update', 'push', 'trukker_tenant', 'push_order_assistant'),
('alert_notification', 'push', 'trukker_tenant', 'push_alert_assistant');

-- Multi-tenant Example (Different tenant)
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('driver_details', 'whatsapp', 'demo_tenant', 'demo_whatsapp_assistant'),
('driver_details', 'sms', 'demo_tenant', 'demo_sms_assistant'),
('customer_support', 'chat', 'demo_tenant', 'demo_chat_assistant');

-- Development/Testing Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('test_api', 'whatsapp', 'test_tenant', 'test_whatsapp_assistant'),
('test_api', 'sms', 'test_tenant', 'test_sms_assistant'),
('debug_api', 'chat', 'dev_tenant', 'debug_chat_assistant');

-- Special Integration Mappings
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id) VALUES
('webhook_callback', 'webhook', 'trukker_tenant', 'webhook_processor_assistant'),
('api_integration', 'rest', 'trukker_tenant', 'rest_api_assistant'),
('batch_processing', 'batch', 'trukker_tenant', 'batch_processor_assistant');

-- Verify the inserted data
SELECT COUNT(*) as total_mappings FROM api_assistant_mapping;

-- Display all mappings for verification
SELECT 
    api_name, 
    communication_mode, 
    tenant_id, 
    assistant_id,
    CONCAT(api_name, '_', communication_mode) as combined_key
FROM api_assistant_mapping 
ORDER BY api_name, communication_mode;

-- Display grouped by communication mode
SELECT 
    communication_mode,
    COUNT(*) as mapping_count
FROM api_assistant_mapping 
GROUP BY communication_mode
ORDER BY mapping_count DESC;

-- Display grouped by tenant
SELECT 
    tenant_id,
    COUNT(*) as mapping_count
FROM api_assistant_mapping 
GROUP BY tenant_id
ORDER BY mapping_count DESC; 