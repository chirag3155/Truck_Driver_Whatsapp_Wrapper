package com.driver.whatsapp.wrapper.config;

import com.driver.whatsapp.wrapper.utils.RestTemplateConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate bean with timeouts configured from WrapperConfiguration
     */
    @Bean
    public RestTemplate restTemplate() {
        return RestTemplateConfig.createRestTemplate();
    }
} 