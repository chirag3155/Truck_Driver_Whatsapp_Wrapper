package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.utils.RestTemplateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class TimeoutManagementService {

    @Autowired
    private ConfigurationCacheService configurationCacheService;

    @Autowired
    private List<RestTemplate> restTemplates;

    /**
     * Refreshes timeouts across all RestTemplate instances after configuration update
     * @return true if all updates were successful, false if any failed
     */
    public boolean refreshTimeouts() {
        try {
            // First reload configurations from database
            configurationCacheService.reloadConfigurations();
            log.info("Reloaded configurations from database");

            // Update all RestTemplate instances
            boolean allSuccess = true;
            for (RestTemplate restTemplate : restTemplates) {
                boolean success = RestTemplateConfig.updateRestTemplate(restTemplate);
                if (!success) {
                    allSuccess = false;
                    log.error("Failed to update timeouts for RestTemplate instance");
                }
            }

            if (allSuccess) {
                log.info("Successfully updated timeouts for all RestTemplate instances");
            } else {
                log.warn("Some RestTemplate instances failed to update timeouts");
            }

            return allSuccess;
        } catch (Exception e) {
            log.error("Error refreshing timeouts: {}", e.getMessage(), e);
            return false;
        }
    }
} 