package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Card.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByEncryptedCardNumber(String encryptedCardNumber);

    Page<Card> findByOwner(User owner, Pageable pageable);

    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);

    List<Card> findByOwnerId(Long ownerId);

    @Query("SELECT c FROM Card c WHERE c.owner = :owner AND c.status = :status")
    List<Card> findActiveCardsByOwner(@Param("owner") User owner, @Param("status") CardStatus status);

    @Query("SELECT c FROM Card c WHERE c.expiryDate < :date AND c.status != :expiredStatus")
    List<Card> findCardsToExpire(@Param("date") LocalDate date, @Param("expiredStatus") CardStatus expiredStatus);

    @Modifying
    @Query("UPDATE Card c SET c.status = :status WHERE c.id = :cardId")
    int updateCardStatus(@Param("cardId") Long cardId, @Param("status") CardStatus status);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.owner = :owner AND c.status = :status")
    long countByOwnerAndStatus(@Param("owner") User owner, @Param("status") CardStatus status);

    @Query("SELECT c FROM Card c WHERE c.owner.id = :userId " +
            "AND (LOWER(c.maskedCardNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(c.cardHolderName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Card> searchUserCards(@Param("userId") Long userId, @Param("searchTerm") String searchTerm, Pageable pageable);

    boolean existsByEncryptedCardNumber(String encryptedCardNumber);
}
