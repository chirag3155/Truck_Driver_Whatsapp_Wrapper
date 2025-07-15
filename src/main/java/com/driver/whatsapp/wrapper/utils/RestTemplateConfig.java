package com.driver.whatsapp.wrapper.utils;

import com.driver.whatsapp.wrapper.constants.ConfigurationConstants;
import com.driver.whatsapp.wrapper.service.ConfigurationCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class RestTemplateConfig {

    /**
     * Creates a new RestTemplate with timeouts from WrapperConfiguration
     */
    public static RestTemplate createRestTemplate() {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            updateTimeouts(factory);
            return new RestTemplate(factory);
        } catch (Exception e) {
            log.error("Error creating RestTemplate with configured timeouts. Using defaults. Error: {}", e.getMessage());
            // Create with default timeouts if configuration fails
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Integer.parseInt(ConfigurationConstants.DEFAULT_CONNECT_TIMEOUT));
            factory.setReadTimeout(Integer.parseInt(ConfigurationConstants.DEFAULT_READ_TIMEOUT));
            return new RestTemplate(factory);
        }
    }

    /**
     * Updates an existing RestTemplate with current timeout values
     * @return true if update was successful, false otherwise
     */
    public static boolean updateRestTemplate(RestTemplate restTemplate) {
        try {
            if (restTemplate == null) {
                log.error("Cannot update null RestTemplate");
                return false;
            }
            
            SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
            updateTimeouts(factory);
            return true;
        } catch (Exception e) {
            log.error("Error updating RestTemplate timeouts: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Updates timeout values on a request factory using values from WrapperConfiguration
     */
    private static void updateTimeouts(SimpleClientHttpRequestFactory factory) {
        int connectTimeout = getConnectTimeout();
        int readTimeout = getReadTimeout();
        
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        log.info("Updated RestTemplate timeouts: connectTimeout={}, readTimeout={}", connectTimeout, readTimeout);
    }

    /**
     * Gets connect timeout from WrapperConfiguration
     */
    private static int getConnectTimeout() {
        try {
            String timeout = ConfigurationCacheService.getConfigValue(
                ConfigurationConstants.REST_CONNECT_TIMEOUT,
                ConfigurationConstants.DEFAULT_CONNECT_TIMEOUT
            );
            log.info("Connect timeout: {}", timeout);
            return Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            log.error("Invalid connect timeout value in configuration. Using default.");
            return Integer.parseInt(ConfigurationConstants.DEFAULT_CONNECT_TIMEOUT);
        }
    }

    /**
     * Gets read timeout from WrapperConfiguration
     */
    private static int getReadTimeout() {
        try {
            String timeout = ConfigurationCacheService.getConfigValue(
                ConfigurationConstants.REST_READ_TIMEOUT,
                ConfigurationConstants.DEFAULT_READ_TIMEOUT
            );
            log.info("Read timeout: {}", timeout);
            return Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            log.error("Invalid read timeout value in configuration. Using default.");
            return Integer.parseInt(ConfigurationConstants.DEFAULT_READ_TIMEOUT);
        }
    }
} 