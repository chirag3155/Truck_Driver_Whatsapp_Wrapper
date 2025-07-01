package com.driver.whatsapp.wrapper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    public static class MessageContent {
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("type")
        private String type;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Price {
        @JsonProperty("pricePerMessage")
        private Double pricePerMessage;
        
        @JsonProperty("currency")
        private String currency;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {
        @JsonProperty("name")
        private String name;
    }
} 