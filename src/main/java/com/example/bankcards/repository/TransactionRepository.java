package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.Transaction.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    @Query("SELECT t FROM Transaction t WHERE t.sourceCard = :card OR t.destinationCard = :card ORDER BY t.transactionDate DESC")
    Page<Transaction> findByCard(@Param("card") Card card, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceCard.owner.id = :userId OR t.destinationCard.owner.id = :userId ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findTransactionsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.sourceCard = :card " +
            "AND t.status = :status AND t.transactionDate >= :startOfDay AND t.transactionDate < :startOfNextDay")
    BigDecimal getTodaySpentAmount(@Param("card") Card card,
                                   @Param("status") TransactionStatus status,
                                   @Param("startOfDay") LocalDateTime startOfDay,
                                   @Param("startOfNextDay") LocalDateTime startOfNextDay);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.sourceCard.owner.id = :userId " +
            "AND t.status = :status AND t.transactionDate >= :fromDate")
    long countUserTransactions(@Param("userId") Long userId,
                               @Param("status") TransactionStatus status,
                               @Param("fromDate") LocalDateTime fromDate);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    boolean existsByTransactionId(String transactionId);
}