-- Fix missing API assistant mapping for OTR flow that's causing scheduler to fail
-- This will allow the scheduler to properly process OTR transactions

-- First, add the next_communication_mode column if it doesn't exist
ALTER TABLE api_assistant_mapping 
ADD COLUMN IF NOT EXISTS next_communication_mode VARCHAR(50) NOT NULL DEFAULT 'calling';

-- Insert the missing OTR mapping for whatsapp communication mode
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id, next_communication_mode, template_message) 
VALUES ('OTR', 'whatsapp', 'IN_TRUKKER_EVA', '1116', 'calling', 
'{
  "templateName": "11_start_time_due",
  "language": "en",
  "placeholders": [
    {"key": "driverName", "defaultValue": "Driver"},
    {"key": "pickUpLocation", "defaultValue": "Pickup Location"},
    {"key": "dropOffLocation", "defaultValue": "Drop Location"},
    {"key": "eta", "defaultValue": "10 AM"}
  ],
  "buttons": [
    {"type": "QUICK_REPLY", "parameter": "Yes I am on time"},
    {"type": "QUICK_REPLY", "parameter": "I am late"}
  ]
}')
ON DUPLICATE KEY UPDATE 
next_communication_mode = VALUES(next_communication_mode),
template_message = VALUES(template_message),
tenant_id = VALUES(tenant_id),
assistant_id = VALUES(assistant_id);

-- Add other flow mappings that might be missing
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id, next_communication_mode, template_message) 
VALUES 
('LOADING_CONFIRMATION', 'whatsapp', 'IN_TRUKKER_EVA', '1116', 'calling', 
'{
  "templateName": "12_loading_confirmation",
  "language": "en",
  "placeholders": [
    {"key": "driverName", "defaultValue": "Driver"},
    {"key": "pickUpLocation", "defaultValue": "Pickup Location"},
    {"key": "commodity", "defaultValue": "Cargo"},
    {"key": "orderNumber", "defaultValue": "Order"}
  ],
  "buttons": [
    {"type": "QUICK_REPLY", "parameter": "Loading Started"},
    {"type": "QUICK_REPLY", "parameter": "Loading Completed"}
  ]
}'),
('ORDER_COMPLETION', 'whatsapp', 'IN_TRUKKER_EVA', '1116', 'calling', 
'{
  "templateName": "13_order_completion",
  "language": "en",
  "placeholders": [
    {"key": "driverName", "defaultValue": "Driver"},
    {"key": "dropOffLocation", "defaultValue": "Drop Location"},
    {"key": "orderNumber", "defaultValue": "Order"},
    {"key": "truckNumber", "defaultValue": "Truck"}
  ],
  "buttons": [
    {"type": "QUICK_REPLY", "parameter": "Order Delivered"},
    {"type": "QUICK_REPLY", "parameter": "Issue with Delivery"}
  ]
}'),
('REMINDER', 'whatsapp', 'IN_TRUKKER_EVA', '1116', 'calling', 
'{
  "templateName": "14_reminder_message",
  "language": "en",
  "placeholders": [
    {"key": "driverName", "defaultValue": "Driver"},
    {"key": "orderNumber", "defaultValue": "Order"},
    {"key": "eta", "defaultValue": "10 AM"},
    {"key": "truckNumber", "defaultValue": "Truck"}
  ],
  "buttons": [
    {"type": "QUICK_REPLY", "parameter": "On Schedule"},
    {"type": "QUICK_REPLY", "parameter": "Delayed"}
  ]
}'),
('STATUS_FOLLOW_UP', 'whatsapp', 'IN_TRUKKER_EVA', '1116', 'calling', 
'{
  "templateName": "15_status_follow_up",
  "language": "en",
  "placeholders": [
    {"key": "driverName", "defaultValue": "Driver"},
    {"key": "orderNumber", "defaultValue": "Order"},
    {"key": "truckNumber", "defaultValue": "Truck"},
    {"key": "statusType", "defaultValue": "Status Update"}
  ],
  "buttons": [
    {"type": "QUICK_REPLY", "parameter": "Update Status"},
    {"type": "QUICK_REPLY", "parameter": "Call Me"}
  ]
}')
ON DUPLICATE KEY UPDATE 
next_communication_mode = VALUES(next_communication_mode),
template_message = VALUES(template_message),
tenant_id = VALUES(tenant_id),
assistant_id = VALUES(assistant_id);

-- Verify the mappings are correct
SELECT 
    api_name, 
    communication_mode, 
    tenant_id, 
    assistant_id, 
    next_communication_mode,
    CASE 
        WHEN template_message IS NOT NULL THEN 'Template Present' 
        ELSE 'No Template' 
    END as template_status
FROM api_assistant_mapping 
WHERE communication_mode = 'whatsapp'
ORDER BY api_name;

-- Show cache key format that ConfigurationCacheService uses
SELECT 
    CONCAT(api_name, '_', communication_mode) as cache_key,
    api_name,
    communication_mode,
    next_communication_mode,
    tenant_id,
    assistant_id
FROM api_assistant_mapping 
WHERE communication_mode = 'whatsapp'
ORDER BY api_name; 