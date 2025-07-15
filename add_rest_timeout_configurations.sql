-- Insert REST connect timeout configuration
INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) 
VALUES ('trukker_wrapper', 'REST_CONNECT_TIMEOUT', 'REST API Connect Timeout (ms)', '30000')
ON CONFLICT (module_name, param_id) DO UPDATE 
SET param_value = EXCLUDED.param_value;

-- Insert REST read timeout configuration
INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) 
VALUES ('trukker_wrapper', 'REST_READ_TIMEOUT', 'REST API Read Timeout (ms)', '30000')
ON CONFLICT (module_name, param_id) DO UPDATE 
SET param_value = EXCLUDED.param_value; 