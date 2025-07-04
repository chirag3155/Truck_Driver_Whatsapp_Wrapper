package com.driver.whatsapp.wrapper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtrBotResponseRequest {
    private String communicationSummary;
    private String transScript;
    private String status;
    private String actionType;
    private String type;
    private String refId;
    private String errorMessage;
    private ExtractPayload extractPayload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtractPayload {
        private String currentStatus;
        private String uniqueId;
        private String tripId;
        private String orderNumber;
        private Boolean isReached;
        private String newETA;
        private Boolean isEmergency;
        private List<String> docPaths;
    }
} 