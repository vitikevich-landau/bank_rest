package com.example.bankcards.repository;

import com.example.bankcards.entity.BlockRequest;
import com.example.bankcards.entity.BlockRequest.RequestStatus;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRequestRepository extends JpaRepository<BlockRequest, Long> {

    Page<BlockRequest> findByStatus(RequestStatus status, Pageable pageable);

    Page<BlockRequest> findByRequestedBy(User requestedBy, Pageable pageable);

    Page<BlockRequest> findByCard(Card card, Pageable pageable);

    @Query("SELECT br FROM BlockRequest br WHERE br.card.id = :cardId AND br.status = :status")
    Optional<BlockRequest> findPendingRequestForCard(@Param("cardId") Long cardId, @Param("status") RequestStatus status);

    @Query("SELECT COUNT(br) FROM BlockRequest br WHERE br.status = :status")
    long countByStatus(@Param("status") RequestStatus status);

    @Query("SELECT br FROM BlockRequest br WHERE br.requestedBy.id = :userId ORDER BY br.requestedAt DESC")
    List<BlockRequest> findUserRequests(@Param("userId") Long userId);

    boolean existsByCardAndStatus(Card card, RequestStatus status);
}