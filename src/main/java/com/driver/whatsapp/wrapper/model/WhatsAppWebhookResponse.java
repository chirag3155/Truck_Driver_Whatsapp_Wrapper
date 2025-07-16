package com.driver.whatsapp.wrapper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookResponse {
    
    @JsonProperty("results")
    private List<Result> results;
    
    @JsonProperty("messageCount")
    private Integer messageCount;
    
    @JsonProperty("pendingMessageCount")
    private Integer pendingMessageCount;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("from")
        private String from;
        
        @JsonProperty("to")
        private String to;
        
        @JsonProperty("integrationType")
        private String integrationType;
        
        @JsonProperty("receivedAt")
        private String receivedAt;
        
        @JsonProperty("messageId")
        private String messageId;
        
        @JsonProperty("callbackData")
        private String callbackData;
        
        @JsonProperty("message")
        private MessageContent message;
        
        @JsonProperty("price")
        private Price price;
        
        @JsonProperty("contact")
        private Contact contact;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageContent {
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("url")
        private String url;

        @JsonProperty("caption") 
        private String caption;
        
        @JsonProperty("type")
        private String type;
        
        // Interactive button reply fields
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("title")
        private String title;
        
        // Explicit getters for document fields
        public String getUrl() {
            return url;
        }
        
        public String getCaption() {
            return caption;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        @JsonProperty("pricePerMessage")
        private Double pricePerMessage;
        
        @JsonProperty("currency")
        private String currency;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        @JsonProperty("name")
        private String name;
    }

    public List<Result> getResults() {
        return results;
    }
} 