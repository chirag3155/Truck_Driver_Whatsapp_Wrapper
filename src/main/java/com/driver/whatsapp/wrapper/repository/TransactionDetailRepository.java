package com.driver.whatsapp.wrapper.repository;

import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.entity.TransactionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, TransactionDetailId> {
    
    /**
     * Find transaction details by phone number
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.phoneNumber = :phoneNumber")
    List<TransactionDetail> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    /**
     * Find transaction details by order number
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.orderNumber = :orderNumber")
    List<TransactionDetail> findByOrderNumber(@Param("orderNumber") String orderNumber);
    
    /**
     * Find transaction details by truck number
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.truckNumber = :truckNumber")
    List<TransactionDetail> findByTruckNumber(@Param("truckNumber") String truckNumber);
    
    /**
     * Find transaction details by transaction ID
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.transactionId = :transactionId")
    Optional<TransactionDetail> findByTransactionId(@Param("transactionId") String transactionId);
    
    /**
     * Find all transactions within a date range (using orderDate field)
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.orderDate BETWEEN :startDate AND :endDate")
    List<TransactionDetail> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find transactions by status code
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.statusCode = :statusCode")
    List<TransactionDetail> findByStatusCode(@Param("statusCode") String statusCode);
    
    /**
     * Find transactions by communication mode
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.communicationMode = :communicationMode")
    List<TransactionDetail> findByCommunicationMode(@Param("communicationMode") String communicationMode);
    
    /**
     * Check if transaction exists for given phone number and order number
     */
    @Query("SELECT COUNT(td) > 0 FROM TransactionDetail td WHERE td.phoneNumber = :phoneNumber AND td.orderNumber = :orderNumber")
    boolean existsByPhoneNumberAndOrderNumber(@Param("phoneNumber") String phoneNumber, @Param("orderNumber") String orderNumber);
    
    /**
     * Find transactions by trip ID
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.tripId = :tripId")
    List<TransactionDetail> findByTripId(@Param("tripId") String tripId);
    
    /**
     * Find transactions by unique ID
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.uniqueId = :uniqueId")
    Optional<TransactionDetail> findByUniqueId(@Param("uniqueId") String uniqueId);
    
    /**
     * Close all other transactions (set state to "closed") except the current one
     * This ensures only one transactions is "open" at a time
     */
    @Modifying
    @Transactional
    @Query("UPDATE TransactionDetail td SET td.state = 'closed' WHERE td.phoneNumber = :phoneNumber AND td.conversationId != :conversationId")
    int closeAllOtherTransactions(@Param("phoneNumber") String phoneNumber, @Param("conversationId") String conversationId);

    /**
     * Find open conversations by driver phone number
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.phoneNumber = :phoneNumber AND td.state = 'open'")
    List<TransactionDetail> findOpenConversationsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Find the most recent open conversation by driver phone number
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.phoneNumber = :phoneNumber AND td.state = 'open' ORDER BY td.transactionTimestamp DESC")
    Optional<TransactionDetail> findMostRecentOpenConversation(@Param("phoneNumber") String phoneNumber);

    /**
     * Find expired conversations based on last activity timestamp
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.state = 'open' AND td.transactionTimestamp < :expirationTime")
    List<TransactionDetail> findExpiredConversations(@Param("expirationTime") LocalDateTime expirationTime);

    /**
     * Update conversation state to closed
     */
    @Modifying
    @Transactional
    @Query("UPDATE TransactionDetail td SET td.state = 'closed', td.updatedTimestamp = :updatedTime WHERE td.phoneNumber = :phoneNumber AND td.conversationId = :conversationId")
    int closeConversation(@Param("phoneNumber") String phoneNumber, @Param("conversationId") String conversationId, @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * Update conversation state to closed for all open conversations of a driver
     */
    @Modifying
    @Transactional
    @Query("UPDATE TransactionDetail td SET td.state = 'closed', td.updatedTimestamp = :updatedTime WHERE td.phoneNumber = :phoneNumber AND td.state = 'open'")
    int closeAllOpenConversations(@Param("phoneNumber") String phoneNumber, @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * Update last activity timestamp for a conversation
     */
    @Modifying
    @Transactional
    @Query("UPDATE TransactionDetail td SET td.updatedTimestamp = :updatedTime WHERE td.phoneNumber = :phoneNumber AND td.conversationId = :conversationId")
    int updateConversationActivity(@Param("phoneNumber") String phoneNumber, @Param("conversationId") String conversationId, @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * Find conversation by conversation ID
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.conversationId = :conversationId")
    Optional<TransactionDetail> findByConversationId(@Param("conversationId") String conversationId);

    /**
     * Find transactions whose transactionTimestamp is older than the given cutoff and state is open
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.transactionTimestamp < :cutoff AND td.state = 'open'")
    List<TransactionDetail> findTransactionsOlderThan(@Param("cutoff") java.time.LocalDateTime cutoff);

    /**
     * Find transaction by transactionId and phoneNumber
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.transactionId = :transactionId AND td.phoneNumber = :phoneNumber")
    Optional<TransactionDetail> findByTransactionIdAndPhoneNumber(@Param("transactionId") String transactionId, @Param("phoneNumber") String phoneNumber);

    /**
     * Find transaction by transactionId, phoneNumber, orderNumber, and truckNumber
     */
    @Query("SELECT td FROM TransactionDetail td WHERE td.transactionId = :transactionId " +
           "AND td.phoneNumber = :phoneNumber " +
           "AND td.orderNumber = :orderNumber " +
           "AND td.truckNumber = :truckNumber")
    Optional<TransactionDetail> findByTransactionIdAndPhoneNumberAndOrderNumberAndTruckNumber(
            @Param("transactionId") String transactionId,
            @Param("phoneNumber") String phoneNumber,
            @Param("orderNumber") String orderNumber,
            @Param("truckNumber") String truckNumber);
} 