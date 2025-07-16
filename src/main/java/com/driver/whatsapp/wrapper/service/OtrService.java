package com.driver.whatsapp.wrapper.service;


import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.entity.TransactionDetailId;
import com.driver.whatsapp.wrapper.model.FlowType;
import com.driver.whatsapp.wrapper.model.OtrRequest;
import com.driver.whatsapp.wrapper.model.TemplateMessage;
import com.driver.whatsapp.wrapper.model.WhatsAppMessage;
import com.driver.whatsapp.wrapper.entity.ApiTemplateMapping;
import com.driver.whatsapp.wrapper.repository.ApiTemplateMappingRepository;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.driver.whatsapp.wrapper.utils.DateTimeFormatUtil;
import com.driver.whatsapp.wrapper.dto.FlowResponse;

@Service
@Slf4j
public class OtrService {

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private ApiTemplateMappingRepository apiTemplateMappingRepository;
    
    @Autowired
    private WhatsAppService whatsAppService;
    
    @Autowired
    private ChatModuleService chatModuleService;
    
    @Autowired
    private DateTimeFormatUtil dateTimeFormatUtil;
    
    @Value("${infobip.whatsapp.from}")
    private String whatsappFromNumber;
    
    @Value("${infobip.api.url}")
    private String infobipApiUrl;
    
    @Value("${infobip.api.key}")
    private String infobipApiKey;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process flow request by looking up transaction ID in database
     * Gets flow_name from existing transaction record
     */
    public FlowResponse processFlowRequestByTransactionId(OtrRequest request, String communicationMode) {
        try {
            String transactionId = request.getTransactionId();
            
            // Find existing transaction by transaction ID
            TransactionDetail existingTransaction = findTransactionByTransactionId(transactionId,request.getPhoneNumber(),request.getOrderNumber(),request.getTruckNumber());
            
            // Validate transaction exists
            if (existingTransaction == null) {
                String errorMsg = "Transaction not found for transaction_id: " + transactionId;
                log.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            // Get flow_name from existing transaction
            String flowName = existingTransaction.getFlowName();
            if (flowName == null || flowName.trim().isEmpty()) {
                String errorMsg = "Flow name is null or empty for transaction_id: " + transactionId;
                log.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            if(request.getConversationId() == null || request.getConversationId().trim().isEmpty()) 
            
            {
                throw new RuntimeException("Conversation ID is required in the request");
            }
            if(!request.getConversationId().equals(existingTransaction.getConversationId())) {
                throw new RuntimeException("Conversation ID does not match the existing transaction");
            }
            log.info("✅ Found existing transaction with flow_name: {} for transaction_id: {}", flowName, transactionId);
            
            // Continue with the existing flow processing logic
            return processFlowRequest(request, flowName, communicationMode);
            
        } catch (RuntimeException e) {
            log.error("❌ Runtime error in processFlowRequestByTransactionId: {}", e.getMessage());
            throw e; // Re-throw runtime exceptions
        } catch (Exception e) {
            log.error("❌ Unexpected error in processFlowRequestByTransactionId: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process flow request: " + e.getMessage(), e);
        }
    }

    /**
     * Find transaction by transaction ID using efficient database query
     */
    private TransactionDetail findTransactionByTransactionId(String transactionId,String phoneNumber,String orderNumber,String truckNumber) {
        try {
            log.info("🔍 Looking up transaction by transaction_id: {}", transactionId);

            // Use efficient repository query to find transaction by transaction_id
            Optional<TransactionDetail> transactionOpt = transactionDetailRepository.findByTransactionIdAndPhoneNumberAndOrderNumberAndTruckNumber(transactionId,phoneNumber,orderNumber,truckNumber);
            
            if (transactionOpt.isPresent()) {
                TransactionDetail transaction = transactionOpt.get();
                log.info("✅ Found transaction: truck={}, order={}, phone={}, flow_name={}", 
                    transaction.getTruckNumber(), transaction.getOrderNumber(), 
                    transaction.getPhoneNumber(), transaction.getFlowName());
                return transaction;
            } else {
                log.warn("⚠️ No transaction found with transaction_id: {}", transactionId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ Error finding transaction by transaction_id {}: {}", transactionId, e.getMessage(), e);
            throw new RuntimeException("Database error while finding transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Process dynamic flow request (OTR, Loading Confirmation, etc.)
     * Original method - now used internally after getting flow_name from database
     */
    public FlowResponse processFlowRequest(OtrRequest request, String flowName, String communicationMode) {
        try {
            // Validate flow type
            if (!FlowType.isValid(flowName)) {
                throw new IllegalArgumentException("Invalid flow type: " + flowName);
            }
            
            FlowType flowType = FlowType.fromValue(flowName);
            
            // Generate conversation ID and transaction ID
            String conversationId = request.getConversationId();
            String transactionId = request.getTransactionId();

            // Get tenantId and assistantId from the request
            String tenantId = request.getTenantId();
            String assistantId = request.getAssistantId();
            
            // Validate required fields
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalArgumentException("tenantId is required in the request");
            }
            if (assistantId == null || assistantId.trim().isEmpty()) {
                throw new IllegalArgumentException("assistantId is required in the request");
            }
            
            // Create template message from database (this is what will be sent to driver)
            List<WhatsAppMessage.Message> templateMessages = createTemplateMessageFromDatabase(request, flowType, communicationMode);
            log.info("Template messages: {}", templateMessages);
            
            // Log and extract template message content for response
            String templateMessageContent = extractAndLogTemplateContent(templateMessages, flowType);
            log.info("Template message content: {}", templateMessageContent);
            // Transaction existence has already been verified by findTransactionByTransactionId
            
            // Close all other transactions, keep only this one as "open"
            // closeAllOtherTransactions(request.getPhoneNumber(), conversationId);
            
            // Send request to chat module to generate response (for logging purposes only)
            String chatResponse = callChatModule(request, tenantId, assistantId, transactionId, conversationId, flowType);
            log.info("Chat module response received for flow: {}, conversation ID: {}", flowName, conversationId);
            log.info("Chat module response content: {}", chatResponse);
            
            
            
            // Send the database template message to driver (not chat module response)
            Map<String, Object> sendResult = sendTemplateMessage(templateMessages);
            boolean messageSent = (Boolean) sendResult.get("success");
            
            // Update status code based on success/failure
            String statusCode = messageSent ? "MESSAGE_SENT" : "MESSAGE_FAILED";
            // transactionDetail.setStatusCode(statusCode);
            // transactionDetailRepository.save(transactionDetail);
            
            if (!messageSent) {
                throw new RuntimeException("Failed to send " + flowName + " message: " + sendResult.get("message"));
            }

            log.info("Flow response: {}", request);
            
            return FlowResponse.builder()
            .transactionId(transactionId)
            .phoneNumber(request.getPhoneNumber())
            .orderNumber(request.getOrderNumber())
            .truckNumber(request.getTruckNumber())
            .uniqueId(request.getUniqueId())
            .tripId(request.getTripId())
            .flowName(flowName)
            .build();
        
            
        } catch (Exception e) {
            log.error("Error processing {} request: {}", flowName, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Generate conversation ID starting with "CON"
     */
    private String generateConversationId() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String random = String.valueOf(new Random().nextInt(9999));
            String input = timestamp + random;
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "CON" + sb.toString().substring(0, 13).toUpperCase(); // CON + 13 chars = 16 total
        } catch (Exception e) {
            log.warn("Error generating conversation ID, using fallback: {}", e.getMessage());
            return "CON" + System.currentTimeMillis() + new Random().nextInt(999);
        }
    }
    
    /**
     * Generate transaction ID with 16 character length
     * Added debug logging to verify uniqueness
     */
    private String generateTransactionId() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String random = String.valueOf(new Random().nextInt(99999));
            String input = timestamp + random;
            
            log.info("🔍 DEBUG: Generating transaction ID with timestamp={}, random={}, input={}", 
                timestamp, random, input);
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String transactionId = sb.toString().substring(0, 16).toUpperCase();
            
            log.info("🔍 DEBUG: Generated transaction ID: {}", transactionId);
            
            return transactionId;
        } catch (Exception e) {
            log.warn("Error generating transaction ID, using fallback: {}", e.getMessage());
            String fallbackId = String.valueOf(System.currentTimeMillis()).substring(0, 16);
            log.info("🔍 DEBUG: Using fallback transaction ID: {}", fallbackId);
            return fallbackId;
        }
    }
    
    /**
     * Close all other transactions when opening a new one
     * This ensures only one transaction is "open" at a time
     */
    // private void closeAllOtherTransactions(String phoneNumber, String conversationId) {
    //     try {
    //         log.info("🔄 Closing all other transactions for conversation: phone={}, conversationId={}", 
    //             phoneNumber, conversationId);
    //         // Update all transactions to "closed" except the current one
    //         int updatedCount = transactionDetailRepository.closeAllOtherTransactions(phoneNumber, conversationId);
            
    //         log.info("✅ Successfully closed {} other transactions", updatedCount);
            
    //     } catch (Exception e) {
    //         log.error("❌ Error closing other transactions: {}", e.getMessage(), e);
    //         // Don't throw exception - this is not critical for the main flow
    //     }
    // }
    
    /**
     * Call chat module to generate response
     */
    private String callChatModule(OtrRequest request, String tenantId, String assistantId, String transactionId, String conversationId, FlowType flowType) {
        try {
            // Generate random message ID
            String messageId = "MSG_" + System.currentTimeMillis() + "_" + new Random().nextInt(9999);
            
            // Create dynamic message content based on flow type
            String messageContent = generateMessageContent(request, flowType);
            
            // Call chat module service with correct parameters
            return chatModuleService.sendMessageToChatModuleWithConfig(
                conversationId,
                request.getPhoneNumber(),
                request.getDriverName(), 
                messageContent,
                messageId,
                System.currentTimeMillis(),
                flowType.getValue(), // Use dynamic flow type instead of hardcoded "whatsapp"
                "whatsapp",
                tenantId,
                assistantId,
                request.getLang()
            );
            
        } catch (Exception e) {
            log.error("Error calling chat module for flow {}: {}", flowType.getValue(), e.getMessage(), e);
            // Return default message if chat module fails
            return generateDefaultMessage(request, flowType);
        }
    }
    
    /**
     * Generate message content based on flow type
     */
    private String generateMessageContent(OtrRequest request, FlowType flowType) {
        switch (flowType) {
            case OTR:
                return String.format(
                    "OTR Update - Order: %s, Truck: %s, Route: %s to %s, ETA: %s, Commodity: %s",
                    request.getOrderNumber(),
                    request.getTruckNumber(),
                    request.getPickUpLocation(),
                    request.getDropOffLocation(),
                    request.getEta(),
                    request.getCommodity()
                );
            case LOADING_CONFIRMATION:
                return String.format(
                    "Loading Confirmation - Order: %s, Truck: %s, Location: %s, Commodity: %s",
                    request.getOrderNumber(),
                    request.getTruckNumber(),
                    request.getPickUpLocation(),
                    request.getCommodity()
                );
            case DOC_REMINDER:
                return String.format(
                    "Document Reminder - Order: %s, Truck: %s, Delivered to: %s, Commodity: %s",
                    request.getOrderNumber(),
                    request.getTruckNumber(),
                    request.getDropOffLocation(),
                    request.getCommodity()
                );
            case REMINDER:
                return String.format(
                    "Reminder - Order: %s, Truck: %s, Route: %s to %s, ETA: %s",
                    request.getOrderNumber(),
                    request.getTruckNumber(),
                    request.getPickUpLocation(),
                    request.getDropOffLocation(),
                    request.getEta()
                );
            case STATUS_FOLLOW_UP:
                return String.format(
                    "Status Follow Up - Order: %s, Truck: %s, Current Status Update Required",
                    request.getOrderNumber(),
                    request.getTruckNumber()
                );
            default:
                return String.format(
                    "Update - Order: %s, Truck: %s",
                    request.getOrderNumber(),
                    request.getTruckNumber()
                );
        }
    }
    
    /**
     * Generate default message if chat module fails
     */
    private String generateDefaultMessage(OtrRequest request, FlowType flowType) {
        switch (flowType) {
            case OTR:
            return "Hello! Your delivery details have been updated. Order: " + request.getOrderNumber() + 
                   ", Truck: " + request.getTruckNumber() + 
                   ", Route: " + request.getPickUpLocation() + " to " + request.getDropOffLocation() + 
                   ". ETA: " + request.getEta() + ". Thank you!";
            case LOADING_CONFIRMATION:
                return "Loading confirmation for Order: " + request.getOrderNumber() + 
                       ", Truck: " + request.getTruckNumber() + " at " + request.getPickUpLocation() + ". Thank you!";
            case DOC_REMINDER:
                return "Document reminder for Order: " + request.getOrderNumber() + 
                       ", Truck: " + request.getTruckNumber() + " delivered to " + request.getDropOffLocation() + ". Thank you!";
            case REMINDER:
                return "Reminder: Order " + request.getOrderNumber() + " for Truck " + request.getTruckNumber() + 
                       " is scheduled. ETA: " + request.getEta() + ". Please update your status.";
            case STATUS_FOLLOW_UP:
                return "Status update required for Order: " + request.getOrderNumber() + 
                       ", Truck: " + request.getTruckNumber() + ". Please provide current status.";
            default:
                return "Update for Order: " + request.getOrderNumber() + ", Truck: " + request.getTruckNumber() + ". Thank you!";
        }
    }
    
    /**
     * Send message based on communication mode
     */
    private boolean sendMessage(OtrRequest request, String chatResponse, String communicationMode) {
        try {
            // Send the chat module response directly via WhatsApp
            whatsAppService.sendTextMessage(request.getPhoneNumber(), chatResponse, request.getOrderNumber());
            log.info("Chat module response sent successfully to: {}", request.getPhoneNumber());
            return true;
        } catch (Exception e) {
            log.error("Failed to send chat module response to {}: {}", request.getPhoneNumber(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create response data object
     */
    private Map<String, Object> createResponseData(OtrRequest request, String chatResponse, boolean messageSent, String statusCode, String transactionId, String conversationId, String flowName, String communicationMode, String templateMessageContent) {
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("transactionId", transactionId);
        data.put("orderNumber", request.getOrderNumber());
        data.put("truckNumber", request.getTruckNumber());
        data.put("phoneNumber", request.getPhoneNumber());
        data.put("tripId", request.getTripId());
        data.put("uniqueId", request.getUniqueId());
        data.put("chatResponse", chatResponse);
        data.put("actualMessageSentToDriver", templateMessageContent); // Clear field name for the actual message
        data.put("messageSent", messageSent);
        data.put("statusCode", statusCode);
        data.put("communicationMode", communicationMode);
        data.put("timestamp", LocalDateTime.now().toString());
        data.put("flowName", flowName);
        return data;
    }

    /**
     * Create template message for different flow types from api_template_mapping
     */
    private List<WhatsAppMessage.Message> createTemplateMessage(OtrRequest request, String chatResponse, FlowType flowType, String communicationMode) {
        try {
            // Get language from request, default to "en" if not provided
            String lang = request.getLang() != null ? request.getLang() : "en";
            
            // Get template name based on flow type
            String templateName = getTemplateNameForFlow(flowType);
            
            // Get template from database - throw exception if not found
            ApiTemplateMapping apiTemplateMapping = apiTemplateMappingRepository
                .findByApiNameAndLangAndTemplateName(flowType.getValue(), lang, templateName)
                .orElseThrow(() -> new RuntimeException(
                    String.format("Template not found in database for flow: %s, lang: %s, template: %s", 
                        flowType.getValue(), lang, templateName)));
            
            // Check if template is active
            if (!apiTemplateMapping.isActive()) {
                throw new RuntimeException(
                    String.format("Template is inactive in database for flow: %s, lang: %s, template: %s", 
                        flowType.getValue(), lang, templateName));
            }
            
            // Parse template JSON - throw exception if invalid
            TemplateMessage templateConfig = parseTemplateFromDatabase(apiTemplateMapping.getTemplate());
            if (templateConfig == null) {
                throw new RuntimeException(
                    String.format("Invalid template JSON in database for flow: %s, lang: %s, template: %s", 
                        flowType.getValue(), lang, templateName));
            }
            
            // Fill placeholders with actual data
            List<String> placeholderValues = fillPlaceholders(templateConfig.getPlaceholders(), request, flowType);
            
            // Convert buttons from config to WhatsApp buttons
            List<WhatsAppMessage.Button> buttons = convertButtons(templateConfig.getButtons());
            
            // Build template message
            WhatsAppMessage.Body body = WhatsAppMessage.Body.builder()
                    .placeholders(placeholderValues)
                    .build();

            WhatsAppMessage.TemplateData templateData = WhatsAppMessage.TemplateData.builder()
                    .body(body)
                    .buttons(buttons)
                    .build();

            WhatsAppMessage.Content content = WhatsAppMessage.Content.builder()
                    .templateName(templateConfig.getTemplateName())
                    .templateData(templateData)
                    .language(templateConfig.getLanguage())
                    .build();

            WhatsAppMessage.Message message = WhatsAppMessage.Message.builder()
                    .from(whatsappFromNumber)
                    .to(request.getPhoneNumber())
                    .messageId(generateMessageId())
                    .content(content)
                    .build();

            return Arrays.asList(message);
            
        } catch (Exception e) {
            log.error("Error creating template message from api_template_mapping: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create template message for flow: " + flowType.getValue() + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse template JSON from database
     */
    private TemplateMessage parseTemplateFromDatabase(String templateJson) {
        try {
            if (templateJson == null || templateJson.trim().isEmpty()) {
                return null;
            }
            return objectMapper.readValue(templateJson, TemplateMessage.class);
        } catch (Exception e) {
            log.error("Error parsing template JSON from database: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Fill placeholders with actual data from request
     */
    private List<String> fillPlaceholders(List<TemplateMessage.PlaceholderConfig> placeholderConfigs, OtrRequest request,FlowType flowType) {
        List<String> values = new ArrayList<>();
        
        for (TemplateMessage.PlaceholderConfig config : placeholderConfigs) {
            String value = getValueForPlaceholderByFlow(config.getKey(), request,flowType);
            if (value == null || value.trim().isEmpty()) {
                value = config.getDefaultValue();
            }                      
            values.add(value);
        }
        
        return values;
    }
    
    /**
     * Get value for placeholder key from request data
     */
    private String getValueForPlaceholderByFlow(String key, OtrRequest request, FlowType flowType) {
        switch (flowType) {
            case OTR:
                return getOtrPlaceholderValue(key, request);
            case LOADING_CONFIRMATION:
                return getLoadingConfirmationPlaceholderValue(key, request);
            case DOC_REMINDER:
                return getDocReminderPlaceholderValue(key, request);
            case REMINDER:
                return getReminderPlaceholderValue(key, request);
            case STATUS_FOLLOW_UP:
                return getStatusFollowUpPlaceholderValue(key, request);
            default:
                return getGenericPlaceholderValue(key, request);
        }
    }

    private String getOtrPlaceholderValue(String key, OtrRequest request) {
        switch (key.toLowerCase()) {
            case "drivername":
                return request.getDriverName() != null ? request.getDriverName() : "Driver";
            case "trucknumber":
                return request.getTruckNumber() != null ? request.getTruckNumber() : "TRK001";
            case "pickuplocation":
                return request.getPickUpLocation() != null ? request.getPickUpLocation() : "Pickup Location";
            case "dropofflocation":
                return request.getDropOffLocation() != null ? request.getDropOffLocation() : "Drop Location";
            case "clientname":
                return request.getClientName() != null ? request.getClientName() : "Client";
            case "movedate":
                if (request.getOrderDate() != null) {
                    try {
                        // Handle both LocalDateTime and String inputs
                        String formatted = dateTimeFormatUtil.formatOrderDate(request.getOrderDate());
                        log.info("�� DEBUG - Order Date formatted: {}", formatted);
                        return formatted;
                    } catch (Exception e) {
                        log.error("❌ Error formatting order date: {}", e.getMessage());
                        return "Today";
                    }
                }
                return "Today";
            case "eta":
                if (request.getEta() != null) {
                    try {
                        // Handle both LocalDateTime and String inputs
                        String formatted = dateTimeFormatUtil.formatEtaTime(request.getEta());
                        log.info("🔍 DEBUG - ETA formatted: {}", formatted);
                        return formatted;
                    } catch (Exception e) {
                        log.error("❌ Error formatting ETA: {}", e.getMessage());
                        return "6:30 PM";
                    }
                }
                return "6:30 PM";
            default:
                return null;
        }
    }

    private String getLoadingConfirmationPlaceholderValue(String key, OtrRequest request) {
        switch (key.toLowerCase()) {
            case "drivername":
                return request.getDriverName() != null ? request.getDriverName() : "Driver";
            case "trucknumber":
                return request.getTruckNumber() != null ? request.getTruckNumber() : "TRK001";
            case "pickuplocation":
                return request.getPickUpLocation() != null ? request.getPickUpLocation() : "Pickup Location";
            case "dropofflocation":
                return request.getDropOffLocation() != null ? request.getDropOffLocation() : "Drop Location";
            case "clientname":
                return request.getClientName() != null ? request.getClientName() : "Client";
            default:
                return null;
        }
    }

    private String getDocReminderPlaceholderValue(String key, OtrRequest request) {
        switch (key.toLowerCase()) {
            case "vendorname":
                return request.getDriverName() != null ? request.getDriverName() : "Driver";
            case "numberoftrips":
                return request.getNumberOfTrips() != null ? request.getNumberOfTrips().toString() : "5";
            default:
                return null;
        }
    }

    private String getReminderPlaceholderValue(String key, OtrRequest request) {
        switch (key.toLowerCase()) {
            case "drivername":
                return request.getDriverName() != null ? request.getDriverName() : "Driver";
            case "trucknumber":
                return request.getTruckNumber() != null ? request.getTruckNumber() : "TRK001";
            case "pickuplocation":
                return request.getPickUpLocation() != null ? request.getPickUpLocation() : "Pickup Location";
            case "dropofflocation":
                return request.getDropOffLocation() != null ? request.getDropOffLocation() : "Drop Location";
            case "commodity":
                return request.getCommodity() != null ? request.getCommodity() : "Cargo";
            case "orderid":
                return request.getOrderNumber() != null ? request.getOrderNumber() : "ORDER123";
            default:
                return null;
        }
    }

    private String getStatusFollowUpPlaceholderValue(String key, OtrRequest request) {
        switch (key.toLowerCase()) {
            case "drivername":
                return request.getDriverName() != null ? request.getDriverName() : "Driver";
            case "trucknumber":
                return request.getTruckNumber() != null ? request.getTruckNumber() : "TRK001";
            case "pickuplocation":
                return request.getPickUpLocation() != null ? request.getPickUpLocation() : "Pickup Location";
            case "dropofflocation":
                return request.getDropOffLocation() != null ? request.getDropOffLocation() : "Drop Location";
            case "clientname":
                return request.getClientName() != null ? request.getClientName() : "Client";
            default:
                return null;
        }
    }

    private String getGenericPlaceholderValue(String key, OtrRequest request) {
        switch (key.toLowerCase()) {
            case "drivername":
                return request.getDriverName() != null ? request.getDriverName() : "Driver";
            case "trucknumber":
                return request.getTruckNumber() != null ? request.getTruckNumber() : "TRK001";
            case "pickuplocation":
                return request.getPickUpLocation() != null ? request.getPickUpLocation() : "Pickup Location";
            case "dropofflocation":
                return request.getDropOffLocation() != null ? request.getDropOffLocation() : "Drop Location";
            case "clientname":
                return request.getClientName() != null ? request.getClientName() : "Client";
            case "eta":
                return request.getEta() != null ? dateTimeFormatUtil.formatEtaTime(request.getEta().toString()) : "6:30 PM";
            case "commodity":
                return request.getCommodity() != null ? request.getCommodity() : "Cargo";
            case "ordernumber":
            case "orderid":
                return request.getOrderNumber() != null ? request.getOrderNumber() : "ORDER123";
            default:
                return null;
        }
    }
    
    /**
     * Convert button configs to WhatsApp buttons
     */
    private List<WhatsAppMessage.Button> convertButtons(List<TemplateMessage.ButtonConfig> buttonConfigs) {
        List<WhatsAppMessage.Button> buttons = new ArrayList<>();
        
        for (TemplateMessage.ButtonConfig config : buttonConfigs) {
            WhatsAppMessage.Button button = WhatsAppMessage.Button.builder()
                    .type(config.getType())
                    .parameter(config.getParameter())
                    .build();
            buttons.add(button);
        }
        
        return buttons;
    }
    

    
    /**
     * Get template name based on flow type
     */
    private String getTemplateNameForFlow(FlowType flowType) {
        switch (flowType) {
            case OTR:
                return "ai_otr_en";
            case LOADING_CONFIRMATION:
                return "ai_loading_confirmation";
            case DOC_REMINDER:
                return "ai_pod_reminder";
            case REMINDER:
                return "ai_reminder_en";
            case STATUS_FOLLOW_UP:
                return "ai_generic_connect_status_update_cross_border";
            default:
                return "ai_generic_connect_status_update_cross_border";
        }
    }
    

    

    
    /**
     * Generate message ID for template messages
     */
    private String generateMessageId() {
        return "TMPL_" + System.currentTimeMillis() + "_" + new Random().nextInt(9999);
    }

    /**
     * Create template message from api_template_mapping without using chat response
     * SIMPLIFIED VERSION - Works for all flows
     */
    private List<WhatsAppMessage.Message> createTemplateMessageFromDatabase(OtrRequest request, FlowType flowType, String communicationMode) {
        try {
            log.info("🔍 Creating template message for flow: {}, communication mode: {}", flowType.getValue(), communicationMode);
            
            // Get language from request, default to "en" if not provided
            String lang = request.getLang() != null ? request.getLang() : "en";
            log.info("📝 Using language: {} for flow: {}", lang, flowType.getValue());
            
            // Get template name based on flow type
            String templateName = getTemplateNameForFlow(flowType);
            log.info("📋 Using template name: {} for flow: {}", templateName, flowType.getValue());
            
            // Get template from database - throw exception if not found
            ApiTemplateMapping apiTemplateMapping = apiTemplateMappingRepository
                .findByApiNameAndLangAndTemplateName(flowType.getValue(), lang, templateName)
                .orElseThrow(() -> new RuntimeException(
                    String.format("Template not found in database for flow: %s, lang: %s, template: %s", 
                        flowType.getValue(), lang, templateName)));
            
            // Check if template is active
            if (!apiTemplateMapping.isActive()) {
                throw new RuntimeException(
                    String.format("Template is inactive in database for flow: %s, lang: %s, template: %s", 
                        flowType.getValue(), lang, templateName));
            }
            
            // Parse template JSON - throw exception if invalid
            TemplateMessage templateConfig = parseTemplateFromDatabase(apiTemplateMapping.getTemplate());
            if (templateConfig == null) {
                throw new RuntimeException(
                    String.format("Invalid template JSON in database for flow: %s, lang: %s, template: %s", 
                        flowType.getValue(), lang, templateName));
            }
            
            log.info("✅ Using template from database for flow: {}, lang: {}, template: {}", 
                flowType.getValue(), lang, templateName);
            
            // Use template from api_template_mapping table
            log.info("📋 Using api_template_mapping template:");
            log.info("- Template Name: {}", templateConfig.getTemplateName());
            log.info("- Language: {}", templateConfig.getLanguage());
            log.info("- Driver: {}", request.getPhoneNumber());
            
            // Fill placeholders with actual data from request
            List<String> placeholderValues = fillPlaceholders(templateConfig.getPlaceholders(), request, flowType);
            log.info("- Placeholder Values: {}", placeholderValues);
            
            // Convert buttons from config to WhatsApp buttons
            List<WhatsAppMessage.Button> buttons = convertButtons(templateConfig.getButtons());
            log.info("Buttons: {}", buttons);
            log.info("- Number of Buttons: {}", buttons.size());
            
            // Build template message
            WhatsAppMessage.Body body = WhatsAppMessage.Body.builder()
                    .placeholders(placeholderValues)
                    .build();

            WhatsAppMessage.TemplateData templateData = WhatsAppMessage.TemplateData.builder()
                    .body(body)
                    .buttons(buttons)
                    .build();

            WhatsAppMessage.Content content = WhatsAppMessage.Content.builder()
                    .templateName(templateConfig.getTemplateName())
                    .templateData(templateData)
                    .language(templateConfig.getLanguage())
                    .build();

            WhatsAppMessage.Message message = WhatsAppMessage.Message.builder()
                    .from(whatsappFromNumber)
                    .to(request.getPhoneNumber())
                    .messageId(generateMessageId())
                    .content(content)
                    .build();

            log.info("✅ Template message created successfully for flow: {}", flowType.getValue());
            return Arrays.asList(message);
            
        } catch (Exception e) {
            log.error("❌ Error creating template message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create template message for flow: " + flowType.getValue() + " - " + e.getMessage(), e);
        }
    }
    

    
    /**
     * Send message to driver via WhatsApp service
     * Returns InfoBip response details
     */
    private Map<String, Object> sendTemplateMessage(List<WhatsAppMessage.Message> templateMessages) {
        Map<String, Object> sendResult = new HashMap<>();
        
        try {
            if (templateMessages == null || templateMessages.isEmpty()) {
                log.error("No messages to send");
                sendResult.put("success", false);
                sendResult.put("message", "No messages to send");
                sendResult.put("infobipResponse", null);
                return sendResult;
            }
            
            for (WhatsAppMessage.Message message : templateMessages) {
                // Determine message type for logging
                boolean isTextMessage = message.getContent().getText() != null;
                String messageType = isTextMessage ? "text" : "template";
                String messageIdentifier = isTextMessage ? 
                    "text content" : 
                    message.getContent().getTemplateName();
                
                log.info("Sending {} message to driver: {} with {}: {}", 
                    messageType, message.getTo(), messageType, messageIdentifier);
                
                // Create WhatsApp message structure
                WhatsAppMessage whatsAppMessage = WhatsAppMessage.builder()
                    .messages(Arrays.asList(message))
                    .build();
                
                // Send the message using WhatsApp service and get InfoBip response
                Map<String, Object> infobipResponse = sendWhatsAppTemplateMessage(whatsAppMessage);
                
                // Check if InfoBip call was successful
                boolean infobipSuccess = (Boolean) infobipResponse.get("success");
                
                if (infobipSuccess) {
                    log.info("{} message sent successfully to: {}", 
                        messageType.substring(0, 1).toUpperCase() + messageType.substring(1), 
                        message.getTo());
                    
                    sendResult.put("success", true);
                    sendResult.put("message", "Message sent successfully via InfoBip");
                    sendResult.put("infobipResponse", infobipResponse);
                } else {
                    log.error("Failed to send {} message to: {} - InfoBip Error: {}", 
                        messageType, message.getTo(), infobipResponse.get("message"));
                    
                    sendResult.put("success", false);
                    sendResult.put("message", "Failed to send message via InfoBip: " + infobipResponse.get("message"));
                    sendResult.put("infobipResponse", infobipResponse);
                }
                
                return sendResult; // Return after first message (assuming single message per request)
            }
            
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
            sendResult.put("success", false);
            sendResult.put("message", "Unexpected error: " + e.getMessage());
            sendResult.put("infobipResponse", null);
        }
        
        return sendResult;
    }
    
    /**
     * Send WhatsApp template message using InfoBip template API only
     * Returns response details instead of throwing exceptions
     */
    private Map<String, Object> sendWhatsAppTemplateMessage(WhatsAppMessage whatsAppMessage) {
        Map<String, Object> infobipResponse = new HashMap<>();
        
        try {
            // Always use template API endpoint
            String endpoint = "/whatsapp/1/message/template";
            String url = infobipApiUrl + endpoint;
            
            log.info("=== INFOBIP API CALL DEBUG ===");
            log.info("InfoBip URL: {}", url);
            log.info("Using Template API Only");
            log.info("InfoBip API Key: {}***", infobipApiKey != null ? infobipApiKey.substring(0, Math.min(8, infobipApiKey.length())) : "NULL");
            
            // Create headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "App " + infobipApiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Convert message to JSON
            String jsonBody = objectMapper.writeValueAsString(whatsAppMessage);
            
            log.info("=== SENDING TO INFOBIP ===");
            log.info("Request URL: {}", url);
            log.info("Request Headers: Authorization=App {}, Content-Type=application/json", 
                infobipApiKey != null ? infobipApiKey.substring(0, Math.min(8, infobipApiKey.length())) + "***" : "NULL");
            log.info("Request Body: {}", jsonBody);
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, headers);

            // Send to InfoBip
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.POST, 
                entity, 
                String.class
            );
            
            log.info("=== INFOBIP RESPONSE ===");
            log.info("Response Status: {}", response.getStatusCode());
            log.info("Response Headers: {}", response.getHeaders());
            log.info("Response Body: {}", response.getBody());
            
            // Build response object
            infobipResponse.put("success", response.getStatusCode().is2xxSuccessful());
            infobipResponse.put("statusCode", response.getStatusCode().value());
            infobipResponse.put("statusMessage", response.getStatusCode().toString());
            infobipResponse.put("responseBody", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Template message sent successfully to InfoBip");
                infobipResponse.put("message", "Template message sent successfully to InfoBip");
            } else {
                log.error("❌ InfoBip API Error - Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                infobipResponse.put("message", "InfoBip API returned error: " + response.getStatusCode());
            }
            
            return infobipResponse;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ InfoBip Client Error (4xx): Status={}, Body={}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            
            infobipResponse.put("success", false);
            infobipResponse.put("statusCode", e.getStatusCode().value());
            infobipResponse.put("statusMessage", e.getStatusCode().toString());
            infobipResponse.put("responseBody", e.getResponseBodyAsString());
            infobipResponse.put("message", "InfoBip Client Error: " + e.getMessage());
            infobipResponse.put("errorType", "CLIENT_ERROR");
            
            return infobipResponse;
            
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("❌ InfoBip Server Error (5xx): Status={}, Body={}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            
            infobipResponse.put("success", false);
            infobipResponse.put("statusCode", e.getStatusCode().value());
            infobipResponse.put("statusMessage", e.getStatusCode().toString());
            infobipResponse.put("responseBody", e.getResponseBodyAsString());
            infobipResponse.put("message", "InfoBip Server Error: " + e.getMessage());
            infobipResponse.put("errorType", "SERVER_ERROR");
            
            return infobipResponse;
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("❌ Network/Connection Error: {}", e.getMessage());
            
            infobipResponse.put("success", false);
            infobipResponse.put("statusCode", 0);
            infobipResponse.put("statusMessage", "Network/Connection Error");
            infobipResponse.put("responseBody", null);
            infobipResponse.put("message", "Cannot reach InfoBip API: " + e.getMessage());
            infobipResponse.put("errorType", "NETWORK_ERROR");
            
            return infobipResponse;
            
        } catch (Exception e) {
            log.error("❌ Unexpected Error sending template message: {}", e.getMessage(), e);
            
            infobipResponse.put("success", false);
            infobipResponse.put("statusCode", 0);
            infobipResponse.put("statusMessage", "Internal Error");
            infobipResponse.put("responseBody", null);
            infobipResponse.put("message", "Unexpected error: " + e.getMessage());
            infobipResponse.put("errorType", "INTERNAL_ERROR");
            
            return infobipResponse;
        }
    }

    /**
     * Extract template content and log it in readable format
     * Show flow-specific format in API response while using template API
     */
    private String extractAndLogTemplateContent(List<WhatsAppMessage.Message> templateMessages, FlowType flowType) {
        if (templateMessages == null || templateMessages.isEmpty()) {
            log.warn("No template messages to extract content from");
            return "No template content available";
        }
        
        try {
            WhatsAppMessage.Message message = templateMessages.get(0);
            WhatsAppMessage.Content content = message.getContent();
            
            if (content == null) {
                log.warn("Template message has no content");
                return "No template content available";
            }
            
            // Generate flow-specific message for API response (what we want to show)
            String flowSpecificMessage = generateFlowSpecificMessageForResponse(message, flowType);
            
            // Log template details
            log.info("=== TEMPLATE MESSAGE BEING SENT ===");
            log.info("Flow Type: {}", flowType.getValue());
            log.info("Template Name: {}", content.getTemplateName());
            log.info("Language: {}", content.getLanguage());
            log.info("To: {}", message.getTo());
            log.info("From: {}", message.getFrom());
            
            // Placeholders
            if (content.getTemplateData() != null && content.getTemplateData().getBody() != null 
                && content.getTemplateData().getBody().getPlaceholders() != null) {
                log.info("Placeholders: {}", content.getTemplateData().getBody().getPlaceholders());
            }
            
            // Buttons
            if (content.getTemplateData() != null && content.getTemplateData().getButtons() != null) {
                log.info("Buttons: {}", content.getTemplateData().getButtons().stream()
                    .map(WhatsAppMessage.Button::getParameter)
                    .reduce((a, b) -> a + ", " + b).orElse("No buttons"));
            }
            
            log.info("Flow-specific format for API response: {}", flowSpecificMessage);
            log.info("=== END TEMPLATE MESSAGE ===");
            
            return flowSpecificMessage; // Return flow-specific format for API response
            
        } catch (Exception e) {
            log.error("Error extracting template content: {}", e.getMessage(), e);
            return "Error extracting template content: " + e.getMessage();
        }
    }
    
    /**
     * Generate flow-specific message format for API response
     * This shows what we want the message to look like, regardless of InfoBip template format
     */
    private String generateFlowSpecificMessageForResponse(WhatsAppMessage.Message message, FlowType flowType) {
        try {
            // Extract placeholders from the template message
            List<String> placeholders = message.getContent().getTemplateData() != null && 
                message.getContent().getTemplateData().getBody() != null ? 
                message.getContent().getTemplateData().getBody().getPlaceholders() : new ArrayList<>();
            
            // Generate flow-specific format based on placeholders
            switch (flowType) {
                case REMINDER:
                    if (placeholders.size() >= 4) {
                        return String.format("📅 Reminder for %s: Order %s is scheduled for %s. Truck: %s. Please provide a status update.",
                            placeholders.get(0), placeholders.get(1), placeholders.get(2), placeholders.get(3));
                    }
                    break;
                    
                case LOADING_CONFIRMATION:
                    if (placeholders.size() >= 4) {
                        return String.format("Hi %s! Please confirm the loading status at %s. Commodity: %s, Order: %s. Are you ready to start loading?",
                            placeholders.get(0), placeholders.get(1), placeholders.get(2), placeholders.get(3));
                    }
                    break;
                    
                case DOC_REMINDER:
                    if (placeholders.size() >= 4) {
                        return String.format("Hello %s! Please submit the required documents for delivery to %s. Order: %s, Truck: %s. Please confirm document status.",
                            placeholders.get(0), placeholders.get(1), placeholders.get(2), placeholders.get(3));
                    }
                    break;
                    
                case STATUS_FOLLOW_UP:
                    if (placeholders.size() >= 4) {
                        return String.format("Hi %s! We need a status update for Order %s, Truck %s. Current status: %s. Please respond.",
                            placeholders.get(0), placeholders.get(1), placeholders.get(2), placeholders.get(3));
                    }
                    break;
                    
                case OTR:
                default:
                    // Use the original template format
                    return generateRenderedMessageContent(message.getContent(), flowType);
            }
            
            // Fallback to template rendering if placeholders don't match
            return generateRenderedMessageContent(message.getContent(), flowType);
            
        } catch (Exception e) {
            log.error("Error generating flow-specific message format: {}", e.getMessage(), e);
            return "Error generating flow-specific format";
        }
    }
    
    /**
     * Generate the actual rendered message content based on template and placeholders
     */
    private String generateRenderedMessageContent(WhatsAppMessage.Content content, FlowType flowType) {
        try {
            List<String> placeholders = content.getTemplateData() != null && 
                content.getTemplateData().getBody() != null ? 
                content.getTemplateData().getBody().getPlaceholders() : new ArrayList<>();
            
            // Use flow type to get correct message format instead of template name
            String messageTemplate = getMessageTemplateFormatByFlow(flowType);
            
            // Replace placeholders in the message template with values
            String renderedMessage = messageTemplate;
            for (int i = 0; i < placeholders.size(); i++) {
                String placeholder = "{" + i + "}";
                if (renderedMessage.contains(placeholder)) {
                    String value = placeholders.get(i);
                    renderedMessage = renderedMessage.replace(placeholder, value);
                }
            }
            
            return renderedMessage;
            
        } catch (Exception e) {
            log.error("Error generating rendered message content: {}", e.getMessage(), e);
            return "Error generating message content";
        }
    }
    
    /**
     * Get message template format based on flow type instead of template name
     * This ensures correct message format even when all flows use same template name
     */
    private String getMessageTemplateFormatByFlow(FlowType flowType) {
        switch (flowType) {
            case OTR:
            return "Hi {0} (Truck: {1}), For your upcoming trip from {2} → {3} for {4} is scheduled on {5}. 👉 Please confirm if you will reach the loading point on time by {6}? Please reply:";
            case LOADING_CONFIRMATION:
                return "Hi {0}! Please confirm the loading status at {1}. Commodity: {2}, Order: {3}. Are you ready to start loading?";
            case DOC_REMINDER:
                return "Hello {0}! Please submit the required documents for delivery to {1}. Order: {2}, Truck: {3}. Please confirm document status.";
            case REMINDER:
                return "📅 Reminder for {0}: Order {1} is scheduled for {2}. Truck: {3}. Please provide a status update.";
            case STATUS_FOLLOW_UP:
                return "Hi {0}! We need a status update for Order {1}, Truck {2}. Current status: {3}. Please respond.";
            default:
                return "Hello {0}! Update for Order {1}. ETA: {2}, Truck: {3}. Please respond with your status.";
        }
    }
}