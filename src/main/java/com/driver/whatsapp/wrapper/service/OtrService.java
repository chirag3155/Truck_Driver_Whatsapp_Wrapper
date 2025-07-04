package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.entity.ApiAssistantMapping;
import com.driver.whatsapp.wrapper.entity.ApiAssistantMappingId;
import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.entity.TransactionDetailId;
import com.driver.whatsapp.wrapper.model.FlowType;
import com.driver.whatsapp.wrapper.model.OtrRequest;
import com.driver.whatsapp.wrapper.model.TemplateMessage;
import com.driver.whatsapp.wrapper.model.WhatsAppMessage;
import com.driver.whatsapp.wrapper.repository.ApiAssistantMappingRepository;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class OtrService {

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private ApiAssistantMappingRepository apiAssistantMappingRepository;
    
    @Autowired
    private WhatsAppService whatsAppService;
    
    @Autowired
    private ChatModuleService chatModuleService;
    
    @Value("${infobip.whatsapp.from}")
    private String whatsappFromNumber;
    
    @Value("${infobip.api.url}")
    private String infobipApiUrl;
    
    @Value("${infobip.api.key}")
    private String infobipApiKey;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process dynamic flow request (OTR, Loading Confirmation, etc.)
     */
    public Map<String, Object> processFlowRequest(OtrRequest request, String flowName, String communicationMode) {
        try {
            // Validate flow type
            if (!FlowType.isValid(flowName)) {
                throw new IllegalArgumentException("Invalid flow type: " + flowName);
            }
            
            FlowType flowType = FlowType.fromValue(flowName);
            
            // Generate conversation ID and transaction ID
            String conversationId = generateConversationId();
            String transactionId = generateTransactionId();

            // Get API assistant mapping dynamically based on flow type and communication mode
            ApiAssistantMapping apiAssistantMapping = apiAssistantMappingRepository
                .findById(new ApiAssistantMappingId(flowName, communicationMode))
                .orElseThrow(() -> new RuntimeException("Api Assistant Mapping not found for flow: " + flowName + " and mode: " + communicationMode));
            
            String tenantId = apiAssistantMapping.getTenantId();
            String assistantId = apiAssistantMapping.getAssistantId();
            
            // Create and save transaction detail
            TransactionDetail transactionDetail = createTransactionDetail(request, transactionId, conversationId, communicationMode);
            transactionDetail.setFlowName(flowName);
            transactionDetailRepository.save(transactionDetail);
            log.info("Transaction detail saved for flow: {}, conversation ID: {}, transaction ID: {}", flowName, conversationId, transactionId);
            
            // ✅ Close all other transactions, keep only this one as "open"
            closeAllOtherTransactions(request.getPhoneNumber(), conversationId);
            
            // Send request to chat module to generate response (for logging purposes only)
            String chatResponse = callChatModule(request, tenantId, assistantId, transactionId, conversationId, flowType);
            log.info("Chat module response received for flow: {}, conversation ID: {}", flowName, conversationId);
            log.info("Chat module response content: {}", chatResponse);
            
            // Create template message from database (this is what will be sent to driver)
            List<WhatsAppMessage.Message> templateMessages = createTemplateMessageFromDatabase(request, flowType, communicationMode);
            log.info("Template messages: {}", templateMessages);
            // Log and extract template message content for response
            String templateMessageContent = extractAndLogTemplateContent(templateMessages, flowType);
            log.info("Template message content: {}", templateMessageContent);
            // Send the database template message to driver (not chat module response)
            Map<String, Object> sendResult = sendTemplateMessage(templateMessages);
            boolean messageSent = (Boolean) sendResult.get("success");
            
            // Update status code based on success/failure
            String statusCode = messageSent ? "MESSAGE_SENT" : "MESSAGE_FAILED";
            transactionDetail.setStatusCode(statusCode);
            transactionDetailRepository.save(transactionDetail);
            
            // Prepare clean response based on InfoBip success/failure
            Map<String, Object> response = new HashMap<>();
            
            if (messageSent) {
                response.put("status", "success");
                response.put("message", flowName + " request processed successfully");
            } else {
                response.put("status", "error");
                response.put("message", "Failed to send " + flowName + " message: " + sendResult.get("message"));
            }
            
            // Only include InfoBip response details
            response.put("infobipResponse", sendResult.get("infobipResponse"));
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing {} request: {}", flowName, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to process " + flowName + " request: " + e.getMessage());
            errorResponse.put("infobipResponse", null);
            return errorResponse;
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
     * Create TransactionDetail entity from OTR request
     * Since transactionId is always unique, we should ALWAYS create new records
     * Added state management: new transaction = "open", others = "closed"
     */
    private TransactionDetail createTransactionDetail(OtrRequest request, String transactionId, String conversationId, String communicationMode) {
        
        log.info("🔍 DEBUG: Creating transaction detail with NEW transactionId: {}", transactionId);
        log.info("🔍 DEBUG: Composite key will be: truck={}, order={}, phone={}, transactionId={}", 
            request.getTruckNumber(), request.getOrderNumber(), request.getPhoneNumber(), transactionId);
        
        TransactionDetailId id = TransactionDetailId.builder()
                .truckNumber(request.getTruckNumber())
                .orderNumber(request.getOrderNumber())
                .phoneNumber(request.getPhoneNumber())
                .transactionId(transactionId)
                .build();
        
        log.info("🔍 DEBUG: Built composite key: {}", id);
        
        // Since transactionId is unique, we should ALWAYS create new records
        // But let's keep the check for debugging purposes
        TransactionDetail existingDetail = transactionDetailRepository.findById(id).orElse(null);
        
        if (existingDetail != null) {
            log.error("❌ UNEXPECTED: Found existing record with NEW transactionId: {}", transactionId);
            log.error("❌ This should NEVER happen! Existing record: {}", existingDetail);
            log.error("❌ Existing record transaction ID: {}", existingDetail.getTransactionId());
            
            // This should never happen, but if it does, update the existing record
            existingDetail.setDriverName(request.getDriverName() != null ? request.getDriverName() : "");
            existingDetail.setDropOffLocation(request.getDropOffLocation());
            existingDetail.setPickUpLocation(request.getPickUpLocation());
            existingDetail.setCommodity(request.getCommodity());
            existingDetail.setOrderDate(request.getOrderDate());
            existingDetail.setEtaTime(request.getEta());
            existingDetail.setUniqueId(request.getUniqueId());
            existingDetail.setTripId(request.getTripId());
            existingDetail.setConversationId(conversationId);
            existingDetail.setStatusCode("PENDING");
            existingDetail.setCommunicationMode(communicationMode);
            existingDetail.setUpdatedTimestamp(LocalDateTime.now());
            existingDetail.setState("open"); // ✅ Set this transaction as OPEN
            
            return existingDetail;
        } else {
            log.info("✅ EXPECTED: No existing record found with transactionId: {}", transactionId);
            log.info("✅ Creating NEW transaction record as expected");
            
            // Create new record (this should ALWAYS happen)
            TransactionDetail newDetail = TransactionDetail.builder()
                .id(id)
                .driverName(request.getDriverName() != null ? request.getDriverName() : "")
                .dropOffLocation(request.getDropOffLocation())
                .pickUpLocation(request.getPickUpLocation())
                .commodity(request.getCommodity())
                .orderDate(request.getOrderDate())
                .etaTime(request.getEta())
                .uniqueId(request.getUniqueId())
                .tripId(request.getTripId())
                .ConversationId(conversationId)
                .statusCode("PENDING")
                .communicationMode(communicationMode)
                .transactionTimestamp(LocalDateTime.now())
                .state("open") // ✅ Set this new transaction as OPEN
                .build();
            
            log.info("✅ Creating new transaction with OPEN state: truck={}, order={}, phone={}, transactionId={}", 
                request.getTruckNumber(), request.getOrderNumber(), request.getPhoneNumber(), transactionId);
            
            return newDetail;
        }
    }
    
    /**
     * Close all other transactions when opening a new one
     * This ensures only one transaction is "open" at a time
     */
    private void closeAllOtherTransactions(String phoneNumber, String conversationId) {
        try {
            log.info("🔄 Closing all other transactions for conversation: phone={}, conversationId={}", 
                phoneNumber, conversationId);
            // Update all transactions to "closed" except the current one
            int updatedCount = transactionDetailRepository.closeAllOtherTransactions(phoneNumber, conversationId);
            
            log.info("✅ Successfully closed {} other transactions", updatedCount);
            
        } catch (Exception e) {
            log.error("❌ Error closing other transactions: {}", e.getMessage(), e);
            // Don't throw exception - this is not critical for the main flow
        }
    }
    
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
                assistantId
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
            case ORDER_COMPLETION:
                return String.format(
                    "Order Completion - Order: %s, Truck: %s, Delivered to: %s, Commodity: %s",
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
            case ORDER_COMPLETION:
                return "Order completed successfully! Order: " + request.getOrderNumber() + 
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
     * Create template message for different flow types from database
     */
    private List<WhatsAppMessage.Message> createTemplateMessage(OtrRequest request, String chatResponse, FlowType flowType, String communicationMode) {
        try {
            // Get API assistant mapping which now includes template data
            ApiAssistantMapping apiAssistantMapping = apiAssistantMappingRepository
                .findById(new ApiAssistantMappingId(flowType.getValue(), communicationMode))
                .orElseThrow(() -> new RuntimeException("Api Assistant Mapping not found for flow: " + flowType.getValue() + " and mode: " + communicationMode));
            
            // Parse template from database
            TemplateMessage templateConfig = parseTemplateFromDatabase(apiAssistantMapping.getTemplateMessage());
            if (templateConfig == null) {
                log.warn("No template found in database for flow: {}, using fallback", flowType.getValue());
                return createFallbackTemplateMessage(request, flowType);
            }
            
            // Fill placeholders with actual data
            List<String> placeholderValues = fillPlaceholders(templateConfig.getPlaceholders(), request,flowType);
            
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
            log.error("Error creating template message from database: {}", e.getMessage(), e);
            return createFallbackTemplateMessage(request, flowType);
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
            case ORDER_COMPLETION:
                return getOrderCompletionPlaceholderValue(key, request);
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
                return request.getOrderDate() != null ? formatDateTimeHumanReadable(request.getOrderDate().toString()) : "Today";
            case "eta":
                return request.getEta() != null ? formatDateTimeHumanReadable(request.getEta().toString()) : "10:00 AM";
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

    private String getOrderCompletionPlaceholderValue(String key, OtrRequest request) {
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
                return request.getEta() != null ? formatDateTimeHumanReadable(request.getEta().toString()) : "10:00 AM";
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
     * Create fallback template message if database template is not available
     */
    private List<WhatsAppMessage.Message> createFallbackTemplateMessage(OtrRequest request, FlowType flowType) {
        // Use the old hardcoded method as fallback
        String templateName = getTemplateNameForFlow(flowType);
        List<String> placeholders = getPlaceholdersForFlow(request, flowType);
        List<WhatsAppMessage.Button> buttons = getButtonsForFlow(flowType);
        
        WhatsAppMessage.Body body = WhatsAppMessage.Body.builder()
                .placeholders(placeholders)
                .build();

        WhatsAppMessage.TemplateData templateData = WhatsAppMessage.TemplateData.builder()
                .body(body)
                .buttons(buttons)
                .build();

        WhatsAppMessage.Content content = WhatsAppMessage.Content.builder()
                .templateName(templateName)
                .templateData(templateData)
                .language("en")
                .build();

        WhatsAppMessage.Message message = WhatsAppMessage.Message.builder()
                .from(whatsappFromNumber)
                .to(request.getPhoneNumber())
                .messageId(generateMessageId())
                .content(content)
                .build();

        return Arrays.asList(message);
    }
    
    /**
     * Get template name based on flow type
     */
    private String getTemplateNameForFlow(FlowType flowType) {
        switch (flowType) {
            case OTR:
                return "al_otr_en";
            case LOADING_CONFIRMATION:
                return "ai_loading_confirmation";
            case ORDER_COMPLETION:
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
     * Get placeholders based on flow type and request data
     */
    private List<String> getPlaceholdersForFlow(OtrRequest request, FlowType flowType) {
        switch (flowType) {
            case OTR:
                return Arrays.asList(
                    request.getDriverName() != null ? request.getDriverName() : "Driver",
                    request.getTruckNumber(),     
                    request.getPickUpLocation(),
                    request.getDropOffLocation(),
                    request.getClientName(),   
                    request.getOrderDate() != null ? formatDateTimeHumanReadable(request.getOrderDate().toString()) : "scheduled time",
                    request.getEta() != null ? formatDateTimeHumanReadable(request.getEta().toString()) : "scheduled time"
                );
            case LOADING_CONFIRMATION:
                return Arrays.asList(
                    request.getDriverName() != null ? request.getDriverName() : "Driver",
                    request.getPickUpLocation(),
                    request.getCommodity() != null ? request.getCommodity() : "Cargo",
                    request.getOrderNumber()
                );
            case ORDER_COMPLETION:
                return Arrays.asList(
                    request.getDriverName() != null ? request.getDriverName() : "Driver",
                    request.getDropOffLocation(),
                    request.getOrderNumber(),
                    request.getTruckNumber()
                );
            case REMINDER:
                return Arrays.asList(
                    request.getDriverName() != null ? request.getDriverName() : "Driver",
                    request.getOrderNumber(),
                    request.getEta() != null ? formatDateTimeHumanReadable(request.getEta().toString()) : "scheduled time",
                    request.getTruckNumber()
                );
            case STATUS_FOLLOW_UP:
                return Arrays.asList(
                    request.getDriverName() != null ? request.getDriverName() : "Driver",
                    request.getOrderNumber(),
                    request.getTruckNumber(),
                    "Status Update"
                );
            default:
                return Arrays.asList(
                    request.getDriverName() != null ? request.getDriverName() : "Driver",
                    request.getPickUpLocation(),
                    request.getDropOffLocation(),
                    request.getEta() != null ? formatDateTimeHumanReadable(request.getEta().toString()) : "scheduled time"
                );
        }
    }
    
    /**
     * Get buttons based on flow type
     */
    private List<WhatsAppMessage.Button> getButtonsForFlow(FlowType flowType) {
        switch (flowType) {
            case OTR:
            return Arrays.asList(
                WhatsAppMessage.Button.builder()
                    .type("QUICK_REPLY")
                    .parameter("Yes, On Time")
                    .build(),
                WhatsAppMessage.Button.builder()
                    .type("QUICK_REPLY")
                    .parameter("No, There is a Delay")
                    .build(),
                WhatsAppMessage.Button.builder()
                    .type("QUICK_REPLY")
                    .parameter("Not loading this trip")
                    .build(),
                WhatsAppMessage.Button.builder()
                    .type("URL")
                    .parameter("https://trukker.com/partner") // Update with actual URL
                    .build()
                );
            case LOADING_CONFIRMATION:
                return Arrays.asList(
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Loading Started")
                        .build(),
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Loading Completed")
                        .build()
                );
            case ORDER_COMPLETION:
                return Arrays.asList(
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Order Delivered")
                        .build(),
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Issue with Delivery")
                        .build()
                );
            case REMINDER:
                return Arrays.asList(
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("On Schedule")
                        .build(),
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Delayed")
                        .build()
                );
            case STATUS_FOLLOW_UP:
                return Arrays.asList(
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Update Status")
                        .build(),
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Call Me")
                        .build()
                );
            default:
                return Arrays.asList(
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Confirm")
                        .build(),
                    WhatsAppMessage.Button.builder()
                        .type("QUICK_REPLY")
                        .parameter("Need Help")
                        .build()
                );
        }
    }
    
    /**
     * Generate message ID for template messages
     */
    private String generateMessageId() {
        return "TMPL_" + System.currentTimeMillis() + "_" + new Random().nextInt(9999);
    }

    /**
     * Create template message from database without using chat response
     * SIMPLIFIED VERSION - Works for all flows
     */
    private List<WhatsAppMessage.Message> createTemplateMessageFromDatabase(OtrRequest request, FlowType flowType, String communicationMode) {
        try {
            log.info("🔍 Creating template message for flow: {}, communication mode: {}", flowType.getValue(), communicationMode);
            
            // Try to get from database first
            ApiAssistantMapping apiAssistantMapping = null;
            TemplateMessage templateConfig = null;
            
            try {
                apiAssistantMapping = apiAssistantMappingRepository
                    .findById(new ApiAssistantMappingId(flowType.getValue(), communicationMode)) 
                    .orElse(null);
                
                if (apiAssistantMapping != null) {
                    templateConfig = parseTemplateFromDatabase(apiAssistantMapping.getTemplateMessage());
                    log.info("✅ Found template in database for flow: {}", flowType.getValue());
                } else {
                    log.warn("⚠️ No database record found for flow: {}, using hardcoded template", flowType.getValue());
                }
            } catch (Exception e) {
                log.warn("⚠️ Database lookup failed for flow: {}, using hardcoded template: {}", flowType.getValue(), e.getMessage());
            }
            
            // If no database template, use hardcoded template (this will work for all flows)
            if (templateConfig == null) {
                log.info("🔄 Using hardcoded template for flow: {}", flowType.getValue());
                return createHardcodedTemplateMessage(request, flowType);
            }
            
            // Use database template
            log.info("📋 Using database template:");
            log.info("- Template Name: {}", templateConfig.getTemplateName());
            log.info("- Language: {}", templateConfig.getLanguage());
            log.info("- Driver: {}", request.getPhoneNumber());
            
            // Fill placeholders with actual data from request
            List<String> placeholderValues = fillPlaceholders(templateConfig.getPlaceholders(), request,flowType);
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
            log.info("🔄 Falling back to hardcoded template for flow: {}", flowType.getValue());
            return createHardcodedTemplateMessage(request, flowType);
        }
    }
    
    /**
     * Create hardcoded template message that works for ALL flows
     * Uses ONLY /whatsapp/1/message/template API with flow-specific content
     */
    private List<WhatsAppMessage.Message> createHardcodedTemplateMessage(OtrRequest request, FlowType flowType) {
        try {
            log.info("🔨 Creating template message for flow: {}", flowType.getValue());
            
            // Always use same template name but with flow-specific placeholders
            String templateName = getTemplateNameForFlow(flowType);
            List<String> placeholders = getPlaceholdersForFlow(request, flowType);
            List<WhatsAppMessage.Button> buttons = getButtonsForFlow(flowType);
            
            log.info("📋 Template message details:");
            log.info("- Template Name: {}", templateName);
            log.info("- Flow Type: {}", flowType.getValue());
            log.info("- Placeholders: {}", placeholders);
            log.info("- Buttons: {}", buttons.size());
            
            // Build template message structure
            WhatsAppMessage.Body body = WhatsAppMessage.Body.builder()
                    .placeholders(placeholders)
                    .build();

            WhatsAppMessage.TemplateData templateData = WhatsAppMessage.TemplateData.builder()
                    .body(body)
                    .buttons(buttons)
                    .build();

            WhatsAppMessage.Content content = WhatsAppMessage.Content.builder()
                    .templateName(templateName)
                    .templateData(templateData)
                    .language("en")
                    .build();

            WhatsAppMessage.Message message = WhatsAppMessage.Message.builder()
                    .from(whatsappFromNumber)
                    .to(request.getPhoneNumber())
                    .messageId(generateMessageId())
                    .content(content)
                    .build();

            log.info("✅ Template message created for flow: {} using template: {}", 
                flowType.getValue(), templateName);
            
            return Arrays.asList(message);
            
        } catch (Exception e) {
            log.error("❌ Error creating template message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create template message for flow: " + flowType.getValue(), e);
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
                    
                case ORDER_COMPLETION:
                    if (placeholders.size() >= 4) {
                        return String.format("Hello %s! Have you completed the delivery to %s? Order: %s, Truck: %s. Please confirm the delivery status.",
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
            case ORDER_COMPLETION:
                return "Hello {0}! Have you completed the delivery to {1}? Order: {2}, Truck: {3}. Please confirm the delivery status.";
            case REMINDER:
                return "📅 Reminder for {0}: Order {1} is scheduled for {2}. Truck: {3}. Please provide a status update.";
            case STATUS_FOLLOW_UP:
                return "Hi {0}! We need a status update for Order {1}, Truck {2}. Current status: {3}. Please respond.";
            default:
                return "Hello {0}! Update for Order {1}. ETA: {2}, Truck: {3}. Please respond with your status.";
        }
    }
    
    /**
     * Format date/time to human readable format like "July 3rd 2pm"
     */
    private String formatDateTimeHumanReadable(String dateTimeStr) {
        try {
            if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
                return "scheduled time";
            }
            
            // Parse the datetime string
            java.time.LocalDateTime dateTime;
            if (dateTimeStr.contains("T")) {
                dateTime = java.time.LocalDateTime.parse(dateTimeStr.substring(0, 19));
            } else {
                dateTime = java.time.LocalDateTime.parse(dateTimeStr);
            }
            
            // Format to "July 3rd 2pm"
            java.time.format.DateTimeFormatter monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM");
            String month = dateTime.format(monthFormatter);
            
            int day = dateTime.getDayOfMonth();
            String dayWithSuffix = day + getDayNumberSuffix(day);
            
            int hour = dateTime.getHour();
            String timeFormat = hour == 0 ? "12am" : 
                               hour < 12 ? hour + "am" :
                               hour == 12 ? "12pm" :
                               (hour - 12) + "pm";
            
            return month + " " + dayWithSuffix + " " + timeFormat;
            
        } catch (Exception e) {
            log.warn("Error formatting datetime '{}': {}", dateTimeStr, e.getMessage());
            return dateTimeStr; // Return original if parsing fails
        }
    }
    
    /**
     * Get day number suffix (st, nd, rd, th)
     */
    private String getDayNumberSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
} 