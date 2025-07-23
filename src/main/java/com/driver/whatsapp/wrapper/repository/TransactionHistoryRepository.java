package com.driver.whatsapp.wrapper.repository;

import com.driver.whatsapp.wrapper.entity.TransactionHistory;
import com.driver.whatsapp.wrapper.entity.TransactionHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, TransactionHistoryId> {

    /**
     * Returns a list of unique conversationIds for the supplied transactionId.
     */
    @Query("SELECT DISTINCT th.conversationId FROM TransactionHistory th WHERE th.transactionId = :transactionId")
    List<String> findDistinctConversationIdsByTransactionId(@Param("transactionId") String transactionId);

    /**
     * Fetches the most recent TransactionHistory entry for a given conversationId.
     */
    Optional<TransactionHistory> findFirstByConversationIdOrderByTransactionTimestampDesc(String conversationId);
} 