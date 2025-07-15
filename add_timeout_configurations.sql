-- Insert read timeout configuration
INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) 
VALUES ('trukker_wrapper', 'READ_TIMEOUT_MS', 'Read Timeout (ms)', '30000')
ON CONFLICT (module_name, param_id) DO UPDATE 
SET param_value = EXCLUDED.param_value;

-- Insert connect timeout configuration
INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) 
VALUES ('trukker_wrapper', 'CONNECT_TIMEOUT_MS', 'Connect Timeout (ms)', '10000')
ON CONFLICT (module_name, param_id) DO UPDATE 
SET param_value = EXCLUDED.param_value; 