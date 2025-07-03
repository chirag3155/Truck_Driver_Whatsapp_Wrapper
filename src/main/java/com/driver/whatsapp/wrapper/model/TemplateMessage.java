package com.driver.whatsapp.wrapper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateMessage {
    
    @JsonProperty("templateName")
    private String templateName;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("placeholders")
    private List<PlaceholderConfig> placeholders;
    
    @JsonProperty("buttons")
    private List<ButtonConfig> buttons;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceholderConfig {
        @JsonProperty("key")
        private String key;
        
        @JsonProperty("defaultValue")
        private String defaultValue;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ButtonConfig {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("parameter")
        private String parameter;
    }
} 