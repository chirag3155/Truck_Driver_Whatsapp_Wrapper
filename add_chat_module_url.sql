-- Add Chat Module API URL configuration with sequential param_id
INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value)
SELECT 
    'trukker_wrapper',
    COALESCE(MAX(CAST(param_id AS UNSIGNED)) + 1, 1),
    'CHAT_MODULE_API_URL',
    'https://eva-integration.bngrenew.com/chat_module/chat'
FROM wrapper_configuration
WHERE module_name = 'trukker_wrapper'; 