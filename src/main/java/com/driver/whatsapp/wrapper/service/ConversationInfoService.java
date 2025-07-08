package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.dto.ConversationInfoRequest;
import com.driver.whatsapp.wrapper.dto.ConversationInfoResponse;
import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import com.driver.whatsapp.wrapper.utils.DateTimeFormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@Slf4j
public class ConversationInfoService {

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private DateTimeFormatUtil dateTimeFormatUtil;

    @Value("${conversation.info.default.enabled:false}")
    private boolean defaultEnabled;

    // Hardcoded default values
    private static final String DEFAULT_DRIVER_NAME = "Savan";
    private static final String DEFAULT_PHONE_NUMBER = "+971000000000";
    private static final String DEFAULT_SOURCE = "Gurgaon";
    private static final String DEFAULT_DESTINATION = "Banglore";
    private static final String DEFAULT_TRUCK_NUMBER = "DEFAULT-TRUCK-001";
    private static final String DEFAULT_ORDER_NUMBER = "DEFAULT-ORDER-001";
    private static final String DEFAULT_TRIP_ID = "DEFAULT-TRIP-001";
    private static final String DEFAULT_FLOW_NAME = "";
    private static final String DEFAULT_COMMUNICATION_MODE = "whatsapp";
    private static final String DEFAULT_CLIENT_NAME = "Malay";

    /**
     * Get conversation information by conversation ID
     */
    public ConversationInfoResponse getConversationInfo(ConversationInfoRequest request) {
        String conversationId = request.getConversationId();
        
        log.info("🔍 Fetching conversation info for conversation ID: {}", conversationId);
        
        try {
            Optional<TransactionDetail> transactionOptional = transactionDetailRepository.findByConversationId(conversationId);
            
            if (transactionOptional.isPresent()) {
                TransactionDetail transaction = transactionOptional.get();
                
                log.info("✅ Found conversation details for ID: {}", conversationId);
                
                // Map entity to response DTO with formatted dates
                ConversationInfoResponse response = ConversationInfoResponse.builder()
                    .conversationId(transaction.getConversationId())
                    .driverName(transaction.getDriverName())
                    .phoneNumber(transaction.getPhoneNumber())
                    .source(transaction.getPickUpLocation())
                    .destination(transaction.getDropOffLocation())
                    .orderDate(dateTimeFormatUtil.formatOrderDate(transaction.getOrderDate()))  // Format order date
                    .eta(dateTimeFormatUtil.formatEtaTime(transaction.getEtaTime()))             // Format ETA time
                    .truckNumber(transaction.getTruckNumber())
                    .orderNumber(transaction.getOrderNumber())
                    .tripId(transaction.getTripId())
                    .flowName(transaction.getFlowName())
                    .communicationMode(transaction.getCommunicationMode())
                    .clientName(transaction.getClientName())
                    .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                    .found(true)
                    .message("Conversation information retrieved successfully")
                    .build();
                
                log.info("📋 Conversation info - Driver: {}, Source: {}, Destination: {}, ETA: {}", 
                    response.getDriverName(), response.getSource(), response.getDestination(), response.getEta());
                
                return response;
            } else {
                log.warn("⚠️ No conversation found for ID: {}", conversationId);
                
                if (defaultEnabled) {
                    log.info("🔄 Using default values for conversation ID: {}", conversationId);
                    // Create default dates and format them
                    LocalDateTime defaultOrderDate = LocalDateTime.now().minusHours(2);
                    LocalDateTime defaultEtaTime = LocalDateTime.now().plusHours(4);
                    
                    return ConversationInfoResponse.builder()
                        .conversationId(conversationId)
                        .driverName(DEFAULT_DRIVER_NAME)
                        .phoneNumber(DEFAULT_PHONE_NUMBER)
                        .source(DEFAULT_SOURCE)
                        .destination(DEFAULT_DESTINATION)
                        .orderDate(dateTimeFormatUtil.formatOrderDate(defaultOrderDate))  // Format default order date
                        .eta(dateTimeFormatUtil.formatEtaTime(defaultEtaTime))             // Format default ETA
                        .truckNumber(DEFAULT_TRUCK_NUMBER)
                        .orderNumber(DEFAULT_ORDER_NUMBER)
                        .tripId(DEFAULT_TRIP_ID)
                        .flowName(DEFAULT_FLOW_NAME)
                        .communicationMode(DEFAULT_COMMUNICATION_MODE)
                        .clientName(DEFAULT_CLIENT_NAME)
                        .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                        .found(true)
                        .message("Default conversation information provided")
                        .build();
                } else {
                    return ConversationInfoResponse.builder()
                        .conversationId(conversationId)
                        .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                        .found(false)
                        .message("No conversation found with the provided conversation ID")
                        .build();
                }
            }
        } catch (Exception e) {
            log.error("❌ Error fetching conversation info for ID {}: {}", conversationId, e.getMessage(), e);
            
            return ConversationInfoResponse.builder()
                .conversationId(conversationId)
                .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                .found(false)
                .message("Error occurred while fetching conversation information: " + e.getMessage())
                .build();
        }
    }

    /**
     * Get conversation information by phone number (most recent open conversation)
     */
    public ConversationInfoResponse getConversationInfoByPhoneNumber(String phoneNumber) {
        log.info("🔍 Fetching conversation info for phone number: {}", phoneNumber);
        
        try {
            Optional<TransactionDetail> transactionOptional = transactionDetailRepository.findMostRecentOpenConversation(phoneNumber);
            
            if (transactionOptional.isPresent()) {
                TransactionDetail transaction = transactionOptional.get();
                
                log.info("✅ Found open conversation for phone: {}", phoneNumber);
                
                // Map entity to response DTO with formatted dates
                ConversationInfoResponse response = ConversationInfoResponse.builder()
                    .conversationId(transaction.getConversationId())
                    .driverName(transaction.getDriverName())
                    .phoneNumber(transaction.getPhoneNumber())
                    .source(transaction.getPickUpLocation())
                    .destination(transaction.getDropOffLocation())
                    .orderDate(dateTimeFormatUtil.formatOrderDate(transaction.getOrderDate()))  // Format order date
                    .eta(dateTimeFormatUtil.formatEtaTime(transaction.getEtaTime()))             // Format ETA time
                    .truckNumber(transaction.getTruckNumber())
                    .orderNumber(transaction.getOrderNumber())
                    .tripId(transaction.getTripId())
                    .flowName(transaction.getFlowName())
                    .communicationMode(transaction.getCommunicationMode())
                    .clientName(DEFAULT_CLIENT_NAME)
                    .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                    .found(true)
                    .message("Conversation information retrieved successfully")
                    .build();
                
                log.info("📋 Conversation info - Driver: {}, Source: {}, Destination: {}, ETA: {}", 
                    response.getDriverName(), response.getSource(), response.getDestination(), response.getEta());
                
                return response;
            } else {
                log.warn("⚠️ No open conversation found for phone: {}", phoneNumber);
                
                if (defaultEnabled) {
                    log.info("🔄 Using default values for phone number: {}", phoneNumber);
                    // Create default dates and format them
                    LocalDateTime defaultOrderDate = LocalDateTime.now().minusHours(2);
                    LocalDateTime defaultEtaTime = LocalDateTime.now().plusHours(4);
                    
                    return ConversationInfoResponse.builder()
                        .phoneNumber(phoneNumber)
                        .driverName(DEFAULT_DRIVER_NAME)
                        .source(DEFAULT_SOURCE)
                        .destination(DEFAULT_DESTINATION)
                        .orderDate(dateTimeFormatUtil.formatOrderDate(defaultOrderDate))  // Format default order date
                        .eta(dateTimeFormatUtil.formatEtaTime(defaultEtaTime))             // Format default ETA
                        .truckNumber(DEFAULT_TRUCK_NUMBER)
                        .orderNumber(DEFAULT_ORDER_NUMBER)
                        .tripId(DEFAULT_TRIP_ID)
                        .flowName(DEFAULT_FLOW_NAME)
                        .communicationMode(DEFAULT_COMMUNICATION_MODE)
                        .clientName(DEFAULT_CLIENT_NAME)
                        .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                        .found(true)
                        .message("Default conversation information provided")
                        .build();
                } else {
                    return ConversationInfoResponse.builder()
                        .phoneNumber(phoneNumber)
                        .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                        .found(false)
                        .message("No open conversation found for the provided phone number")
                        .build();
                }
            }
        } catch (Exception e) {
            log.error("❌ Error fetching conversation info for phone {}: {}", phoneNumber, e.getMessage(), e);
            
            return ConversationInfoResponse.builder()
                .phoneNumber(phoneNumber)
                .currentDatetime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
                .found(false)
                .message("Error occurred while fetching conversation information: " + e.getMessage())
                .build();
        }
    }
} 