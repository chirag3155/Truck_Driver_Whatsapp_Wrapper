package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.dto.ConversationInfoRequest;
import com.driver.whatsapp.wrapper.dto.ConversationInfoResponse;
import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ConversationInfoService {

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

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
                
                // Map entity to response DTO
                ConversationInfoResponse response = ConversationInfoResponse.builder()
                    .conversationId(transaction.getConversationId())
                    .driverName(transaction.getDriverName())
                    .phoneNumber(transaction.getPhoneNumber())
                    .source(transaction.getPickUpLocation())
                    .destination(transaction.getDropOffLocation())
                    .eta(transaction.getEtaTime())
                    .truckNumber(transaction.getTruckNumber())
                    .orderNumber(transaction.getOrderNumber())
                    .tripId(transaction.getTripId())
                    .flowName(transaction.getFlowName())
                    .communicationMode(transaction.getCommunicationMode())
                    .state(transaction.getState())
                    .lastActivity(transaction.getUpdatedTimestamp() != null ? 
                        transaction.getUpdatedTimestamp() : transaction.getTransactionTimestamp())
                    .found(true)
                    .message("Conversation information retrieved successfully")
                    .build();
                
                log.info("📋 Conversation info - Driver: {}, Source: {}, Destination: {}, ETA: {}", 
                    response.getDriverName(), response.getSource(), response.getDestination(), response.getEta());
                
                return response;
            } else {
                log.warn("⚠️ No conversation found for ID: {}", conversationId);
                
                return ConversationInfoResponse.builder()
                    .conversationId(conversationId)
                    .found(false)
                    .message("No conversation found with the provided conversation ID")
                    .build();
            }
        } catch (Exception e) {
            log.error("❌ Error fetching conversation info for ID {}: {}", conversationId, e.getMessage(), e);
            
            return ConversationInfoResponse.builder()
                .conversationId(conversationId)
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
                
                // Map entity to response DTO
                ConversationInfoResponse response = ConversationInfoResponse.builder()
                    .conversationId(transaction.getConversationId())
                    .driverName(transaction.getDriverName())
                    .phoneNumber(transaction.getPhoneNumber())
                    .source(transaction.getPickUpLocation())
                    .destination(transaction.getDropOffLocation())
                    .eta(transaction.getEtaTime())
                    .truckNumber(transaction.getTruckNumber())
                    .orderNumber(transaction.getOrderNumber())
                    .tripId(transaction.getTripId())
                    .flowName(transaction.getFlowName())
                    .communicationMode(transaction.getCommunicationMode())
                    .state(transaction.getState())
                    .lastActivity(transaction.getUpdatedTimestamp() != null ? 
                        transaction.getUpdatedTimestamp() : transaction.getTransactionTimestamp())
                    .found(true)
                    .message("Conversation information retrieved successfully")
                    .build();
                
                log.info("📋 Conversation info - Driver: {}, Source: {}, Destination: {}, ETA: {}", 
                    response.getDriverName(), response.getSource(), response.getDestination(), response.getEta());
                
                return response;
            } else {
                log.warn("⚠️ No open conversation found for phone: {}", phoneNumber);
                
                return ConversationInfoResponse.builder()
                    .phoneNumber(phoneNumber)
                    .found(false)
                    .message("No open conversation found for the provided phone number")
                    .build();
            }
        } catch (Exception e) {
            log.error("❌ Error fetching conversation info for phone {}: {}", phoneNumber, e.getMessage(), e);
            
            return ConversationInfoResponse.builder()
                .phoneNumber(phoneNumber)
                .found(false)
                .message("Error occurred while fetching conversation information: " + e.getMessage())
                .build();
        }
    }
} 