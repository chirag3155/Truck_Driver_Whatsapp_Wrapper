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
public class WhatsAppMessage {
    
    @JsonProperty("messages")
    private List<Message> messages;
    
    // Simple text message class for direct API calls (without messages array)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleTextMessage {
        @JsonProperty("from")
        private String from;
        
        @JsonProperty("to")
        private String to;
        
        @JsonProperty("messageId")
        private String messageId;
        
        @JsonProperty("content")
        private SimpleContent content;
        
        @JsonProperty("callbackData")
        private String callbackData;
        
        @JsonProperty("notifyUrl")
        private String notifyUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleContent {
        @JsonProperty("text")
        private String text;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        @JsonProperty("from")
        private String from;
        
        @JsonProperty("to")
        private String to;
        
        @JsonProperty("messageId")
        private String messageId;
        
        @JsonProperty("content")
        private Content content;
        
        @JsonProperty("callbackData")
        private String callbackData;
        
        @JsonProperty("notifyUrl")
        private String notifyUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Content {
        @JsonProperty("templateName")
        private String templateName;
        
        @JsonProperty("templateData")
        private TemplateData templateData;
        
        @JsonProperty("language")
        private String language;
        
        @JsonProperty("text")
        private String text;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateData {
        @JsonProperty("body")
        private Body body;
        
        @JsonProperty("buttons")
        private List<Button> buttons;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Body {
        @JsonProperty("placeholders")
        private List<String> placeholders;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Button {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("parameter")
        private String parameter;
    }
    
    // Interactive Button Message classes for InfoBip interactive button API
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteractiveButtonMessage {
        @JsonProperty("from")
        private String from;
        
        @JsonProperty("to")
        private String to;
        
        @JsonProperty("messageId")
        private String messageId;
        
        @JsonProperty("content")
        private InteractiveContent content;
        
        @JsonProperty("callbackData")
        private String callbackData;
        
        @JsonProperty("notifyUrl")
        private String notifyUrl;
        
        @JsonProperty("urlOptions")
        private UrlOptions urlOptions;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteractiveContent {
        @JsonProperty("body")
        private InteractiveBody body;
        
        @JsonProperty("action")
        private InteractiveAction action;
        
        @JsonProperty("footer")
        private InteractiveFooter footer;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteractiveBody {
        @JsonProperty("text")
        private String text;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteractiveAction {
        @JsonProperty("buttons")
        private List<InteractiveButton> buttons;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteractiveButton {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("title")
        private String title;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteractiveFooter {
        @JsonProperty("text")
        private String text;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UrlOptions {
        @JsonProperty("shortenUrl")
        private Boolean shortenUrl;
        
        @JsonProperty("trackClicks")
        private Boolean trackClicks;
        
        @JsonProperty("trackingUrl")
        private String trackingUrl;
        
        @JsonProperty("removeProtocol")
        private Boolean removeProtocol;
    }
} 