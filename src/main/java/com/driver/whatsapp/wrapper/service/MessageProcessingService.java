package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.model.WhatsAppWebhookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MessageProcessingService {

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private TruKKerService truKKerService;

    // Store conversation state for each driver (In production, use Redis or database)
    private final Map<String, ConversationState> conversationStates = new HashMap<>();

    /**
     * Process incoming WhatsApp message from driver
     */
    public void processDriverMessage(WhatsAppWebhookResponse webhookResponse) {
        for (WhatsAppWebhookResponse.Result result : webhookResponse.getResults()) {
            String driverPhone = result.getFrom();
            String messageContent = getMessageContent(result.getMessage());
            String orderId = result.getMessageId();

            log.info("Processing message from driver: {}, Content: {}, OrderId: {}", 
                    driverPhone, messageContent, orderId);

            // Get or create conversation state
            ConversationState state = conversationStates.getOrDefault(driverPhone, 
                    new ConversationState(driverPhone, orderId));
            conversationStates.put(driverPhone, state);

            processMessageBasedOnState(state, messageContent, driverPhone, orderId);
        }
    }

    /**
     * Process message based on current conversation state
     */
    private void processMessageBasedOnState(ConversationState state, String messageContent, 
                                           String driverPhone, String orderId) {
        
        log.info("Current conversation step for driver {}: {}", driverPhone, state.getCurrentStep());
        
        switch (state.getCurrentStep()) {
            case INITIAL_ETA_CHECK:
                log.info("Handling INITIAL_ETA_CHECK for driver: {}", driverPhone);
                handleInitialEtaResponse(state, messageContent, driverPhone, orderId);
                break;
            case WAITING_FOR_NEW_ETA:
                log.info("Handling WAITING_FOR_NEW_ETA for driver: {}", driverPhone);
                handleNewEtaResponse(state, messageContent, driverPhone, orderId);
                break;
            case WAITING_FOR_ISSUE_TYPE:
                log.info("Handling WAITING_FOR_ISSUE_TYPE for driver: {}", driverPhone);
                handleIssueTypeResponse(state, messageContent, driverPhone, orderId);
                break;
            case WAITING_FOR_BREAKDOWN_DETAILS:
                log.info("Handling WAITING_FOR_BREAKDOWN_DETAILS for driver: {}", driverPhone);
                handleBreakdownDetailsResponse(state, messageContent, driverPhone, orderId);
                break;
            case COMPLETED:
                log.info("Conversation already completed for driver: {}", driverPhone);
                whatsAppService.sendTextMessage(driverPhone, 
                    "Thank you! Your status has already been updated. If you need further assistance, please contact our support team.", orderId);
                break;
            default:
                log.warn("Falling back to handleGeneralResponse for driver: {} with state: {}", driverPhone, state.getCurrentStep());
                handleGeneralResponse(state, messageContent, driverPhone, orderId);
        }
    }

    /**
     * Handle initial ETA check response (Yes/No)
     */
    private void handleInitialEtaResponse(ConversationState state, String messageContent, 
                                        String driverPhone, String orderId) {
        
        log.info("Checking message: '{}' for positive/negative response", messageContent);
        boolean isPositive = containsPositiveResponse(messageContent);
        boolean isNegative = containsNegativeResponse(messageContent);
        log.info("Positive response: {}, Negative response: {}", isPositive, isNegative);
        
        if (isPositive) {
            // Driver is on time
            log.info("Driver {} confirmed on time for order: {}", driverPhone, orderId);
            truKKerService.updateDriverStatusOnTime(orderId, driverPhone);
            whatsAppService.sendConfirmationMessage(driverPhone, 
                    "Thank you for confirming! You're marked as on time. Have a safe journey!", orderId);
            state.setCurrentStep(ConversationStep.COMPLETED);
            
        } else if (isNegative) {
            // Driver is late, ask for more details
            log.info("Driver {} reported delay for order: {}", driverPhone, orderId);
            whatsAppService.sendTextMessage(driverPhone, 
                    "We understand you're facing a delay. Please let us know:\n" +
                    "1. Traffic delay\n" +
                    "2. Vehicle breakdown\n" +
                    "3. Other issue\n\n" +
                    "Just reply with the number (1, 2, or 3) or describe your issue.", orderId);
            state.setCurrentStep(ConversationStep.WAITING_FOR_ISSUE_TYPE);
            
        } else {
            // Unclear response, ask again
            log.info("Unclear response from driver {}: '{}'", driverPhone, messageContent);
            whatsAppService.sendTextMessage(driverPhone, 
                    "I didn't understand your response. Please reply with:\n" +
                    "- 'Yes' if you're on time\n" +
                    "- 'No' if you're facing delays", orderId);
        }
    }

    /**
     * Handle issue type response
     */
    private void handleIssueTypeResponse(ConversationState state, String messageContent, 
                                       String driverPhone, String orderId) {
        
        if (messageContent.contains("1") || containsTrafficKeywords(messageContent)) {
            // Traffic delay
            truKKerService.updateDriverStatusDelayed(orderId, driverPhone, "Traffic delay");
            whatsAppService.sendNewEtaRequest(driverPhone, orderId);
            state.setCurrentStep(ConversationStep.WAITING_FOR_NEW_ETA);
            
        } else if (messageContent.contains("2") || containsBreakdownKeywords(messageContent)) {
            // Vehicle breakdown
            whatsAppService.sendTextMessage(driverPhone, 
                    "Sorry to hear about the breakdown. Please provide more details about the issue so we can assist you better.", orderId);
            state.setCurrentStep(ConversationStep.WAITING_FOR_BREAKDOWN_DETAILS);
            
        } else if (messageContent.contains("3") || messageContent.toLowerCase().contains("other")) {
            // Other issue
            whatsAppService.sendTextMessage(driverPhone, 
                    "Please describe the issue you're facing and provide your new estimated arrival time.", orderId);
            state.setCurrentStep(ConversationStep.WAITING_FOR_NEW_ETA);
            
        } else {
            // Try to extract time directly if they provided ETA
            String extractedTime = extractTimeFromMessage(messageContent);
            if (extractedTime != null) {
                handleNewEtaResponse(state, messageContent, driverPhone, orderId);
            } else {
                whatsAppService.sendTextMessage(driverPhone, 
                        "Please select:\n1. Traffic delay\n2. Vehicle breakdown\n3. Other issue", orderId);
            }
        }
    }

    /**
     * Handle new ETA response
     */
    private void handleNewEtaResponse(ConversationState state, String messageContent, 
                                    String driverPhone, String orderId) {
        
        String newEta = extractTimeFromMessage(messageContent);
        
        if (newEta != null) {
            // Valid ETA provided
            truKKerService.updateNewEta(orderId, newEta, driverPhone);
            whatsAppService.sendConfirmationMessage(driverPhone, 
                    String.format("Thank you! We've updated your new ETA to %s. " +
                                "The system has been notified. Stay safe!", newEta), orderId);
            state.setCurrentStep(ConversationStep.COMPLETED);
            
        } else {
            // Invalid ETA format
            whatsAppService.sendTextMessage(driverPhone, 
                    "Please provide a valid time format (e.g., 10:30 AM, 2:15 PM, or 14:30).", orderId);
        }
    }

    /**
     * Handle breakdown details response
     */
    private void handleBreakdownDetailsResponse(ConversationState state, String messageContent, 
                                              String driverPhone, String orderId) {
        
        truKKerService.updateDriverStatusBreakdown(orderId, driverPhone, messageContent);
        whatsAppService.sendConfirmationMessage(driverPhone, 
                "Thank you for the details. We've notified the system about the breakdown. " +
                "Our support team will contact you shortly to assist with the situation.", orderId);
        state.setCurrentStep(ConversationStep.COMPLETED);
    }

    /**
     * Handle general responses
     */
    private void handleGeneralResponse(ConversationState state, String messageContent, 
                                     String driverPhone, String orderId) {
        
        whatsAppService.sendTextMessage(driverPhone, 
                "Thank you for your message. If you need to update your status, " +
                "please contact our support team.", orderId);
    }

    /**
     * Extract message content from webhook response
     */
    private String getMessageContent(WhatsAppWebhookResponse.MessageContent message) {
        if (message != null && message.getText() != null) {
            return message.getText();
        }
        return "";
    }

    /**
     * Check if message contains positive response
     */
    private boolean containsPositiveResponse(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("yes") || 
               lowerMessage.contains("on time") || 
               lowerMessage.contains("okay") ||
               lowerMessage.contains("ok") ||
               lowerMessage.contains("fine") ||
               lowerMessage.contains("good");
    }

    /**
     * Check if message contains negative response
     */
    private boolean containsNegativeResponse(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("no") || 
               lowerMessage.contains("late") || 
               lowerMessage.contains("delay") ||
               lowerMessage.contains("behind") ||
               lowerMessage.contains("problem");
    }

    /**
     * Check if message contains traffic-related keywords
     */
    private boolean containsTrafficKeywords(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("traffic") || 
               lowerMessage.contains("jam") || 
               lowerMessage.contains("road") ||
               lowerMessage.contains("congestion");
    }

    /**
     * Check if message contains breakdown-related keywords
     */
    private boolean containsBreakdownKeywords(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("breakdown") || 
               lowerMessage.contains("break down") || 
               lowerMessage.contains("vehicle") ||
               lowerMessage.contains("truck") ||
               lowerMessage.contains("engine") ||
               lowerMessage.contains("tire") ||
               lowerMessage.contains("mechanical");
    }

    /**
     * Extract time from message using regex
     */
    private String extractTimeFromMessage(String message) {
        // Patterns for different time formats
        Pattern[] timePatterns = {
            Pattern.compile("\\b(\\d{1,2}:\\d{2}\\s*(?:AM|PM))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d{1,2}:\\d{2})\\b"),
            Pattern.compile("\\b(\\d{1,2}\\s*(?:AM|PM))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d{1,2}\\.\\d{2})\\b")
        };

        for (Pattern pattern : timePatterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Conversation state enum
     */
    public enum ConversationStep {
        INITIAL_ETA_CHECK,
        WAITING_FOR_ISSUE_TYPE,
        WAITING_FOR_NEW_ETA,
        WAITING_FOR_BREAKDOWN_DETAILS,
        COMPLETED
    }

    /**
     * Conversation state class
     */
    public static class ConversationState {
        private String driverPhone;
        private String orderId;
        private ConversationStep currentStep;

        public ConversationState(String driverPhone, String orderId) {
            this.driverPhone = driverPhone;
            this.orderId = orderId;
            this.currentStep = ConversationStep.INITIAL_ETA_CHECK;
        }

        // Getters and setters
        public String getDriverPhone() { return driverPhone; }
        public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
        
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        
        public ConversationStep getCurrentStep() { return currentStep; }
        public void setCurrentStep(ConversationStep currentStep) { this.currentStep = currentStep; }
    }
} 