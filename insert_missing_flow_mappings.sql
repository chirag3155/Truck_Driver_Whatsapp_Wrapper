-- Insert missing API assistant mappings for all flow types

-- LOADING_CONFIRMATION + WhatsApp
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id, next_communication_mode, template_message) 
VALUES ('LOADING_CONFIRMATION', 'whatsapp', 'EG_TRU_a5d34a6f', '1123', 'whatsapp', 
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
}')
ON DUPLICATE KEY UPDATE 
template_message = VALUES(template_message),
tenant_id = VALUES(tenant_id),
assistant_id = VALUES(assistant_id);

-- ORDER_COMPLETION + WhatsApp
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id, next_communication_mode, template_message) 
VALUES ('ORDER_COMPLETION', 'whatsapp', 'EG_TRU_a5d34a6f', '1123', 'whatsapp', 
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
}')
ON DUPLICATE KEY UPDATE 
template_message = VALUES(template_message),
tenant_id = VALUES(tenant_id),
assistant_id = VALUES(assistant_id);

-- REMINDER + WhatsApp
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id, next_communication_mode, template_message) 
VALUES ('REMINDER', 'whatsapp', 'EG_TRU_a5d34a6f', '1123', 'whatsapp', 
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
}')
ON DUPLICATE KEY UPDATE 
template_message = VALUES(template_message),
tenant_id = VALUES(tenant_id),
assistant_id = VALUES(assistant_id);

-- STATUS_FOLLOW_UP + WhatsApp
INSERT INTO api_assistant_mapping (api_name, communication_mode, tenant_id, assistant_id, next_communication_mode, template_message) 
VALUES ('STATUS_FOLLOW_UP', 'whatsapp', 'EG_TRU_a5d34a6f', '1123', 'whatsapp', 
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
template_message = VALUES(template_message),
tenant_id = VALUES(tenant_id),
assistant_id = VALUES(assistant_id);

-- Verify all records are inserted
SELECT api_name, communication_mode, tenant_id, assistant_id, 
       CASE 
         WHEN template_message IS NOT NULL THEN 'Template Present' 
         ELSE 'No Template' 
       END as template_status
FROM api_assistant_mapping 
WHERE communication_mode = 'whatsapp'
ORDER BY api_name; 