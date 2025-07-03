-- Add template_message column to api_assistant_mapping table
ALTER TABLE api_assistant_mapping 
ADD COLUMN template_message JSON;

-- Update the table with template data for each flow type
UPDATE api_assistant_mapping 
SET template_message = '{
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
}'
WHERE api_name = 'OTR';

UPDATE api_assistant_mapping 
SET template_message = '{
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
}'
WHERE api_name = 'LOADING_CONFIRMATION';

UPDATE api_assistant_mapping 
SET template_message = '{
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
}'
WHERE api_name = 'ORDER_COMPLETION';

UPDATE api_assistant_mapping 
SET template_message = '{
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
}'
WHERE api_name = 'REMINDER';

UPDATE api_assistant_mapping 
SET template_message = '{
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
}'
WHERE api_name = 'STATUS_FOLLOW_UP'; 