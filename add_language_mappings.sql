-- Add language mappings to wrapper_configuration table
-- This script adds language code to language name mappings for the wrapper

-- Delete existing language mappings if they exist
DELETE FROM wrapper_configuration 
WHERE module_name = 'trukker_wrapper' 
AND param_name IN ('en', 'ar', 'tr', 'hi') 
AND param_id LIKE 'lang_%';

-- Delete default language setting if exists
DELETE FROM wrapper_configuration 
WHERE module_name = 'trukker_wrapper' 
AND param_id = 'default_language_id';

-- ==================== LANGUAGE MAPPINGS ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'lang_en', 'en', 'English'),
('trukker_wrapper', 'lang_ar', 'ar', 'Arabic'),
('trukker_wrapper', 'lang_tr', 'tr', 'Turkish'),
('trukker_wrapper', 'lang_hi', 'hi', 'Hindi');

-- ==================== DEFAULT LANGUAGE SETTING ====================

INSERT INTO wrapper_configuration (module_name, param_id, param_name, param_value) VALUES
('trukker_wrapper', 'default_language_id', 'Default Language ID', 'en');

-- Verify the insertions
SELECT param_id, param_name, param_value FROM wrapper_configuration 
WHERE module_name = 'trukker_wrapper' 
AND (param_name IN ('en', 'ar', 'tr', 'hi') OR param_id = 'default_language_id')
ORDER BY param_name; 