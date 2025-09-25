package com.example.bankcards.service;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.entity.*;
import com.example.bankcards.entity.Card.CardStatus;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.*;
import com.example.bankcards.util.CardNumberUtils;
import com.example.bankcards.util.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final BlockRequestRepository blockRequestRepository;
    private final TransactionRepository transactionRepository;
    private final CardNumberUtils cardNumberUtils;
    private final EncryptionUtils encryptionUtils;

    @Value("${app.card.default-limit}")
    private BigDecimal defaultLimit;

    @Transactional
    public CardDto createCard(String username, CreateCardRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String cardNumber = cardNumberUtils.generateCardNumber();
        String cvv = cardNumberUtils.generateCVV();

        Card card = Card.builder()
                .encryptedCardNumber(encryptionUtils.encrypt(cardNumber))
                .maskedCardNumber(cardNumberUtils.maskCardNumber(cardNumber))
                .cardHolderName(request.getCardHolderName() != null ?
                        request.getCardHolderName() : user.getFullName())
                .expiryDate(LocalDate.now().plusYears(3))
                .encryptedCvv(encryptionUtils.encrypt(cvv))
                .status(CardStatus.ACTIVE)
                .cardType(request.getCardType())
                .balance(request.getInitialBalance() != null ?
                        request.getInitialBalance() : BigDecimal.ZERO)
                .dailyLimit(request.getDailyLimit() != null ?
                        request.getDailyLimit() : defaultLimit)
                .owner(user)
                .build();

        Card savedCard = cardRepository.save(card);
        log.info("Card created for user: {} with masked number: {}",
                username, savedCard.getMaskedCardNumber());

        return mapToCardDto(savedCard);
    }

    @Transactional(readOnly = true)
    public CardDto getCardById(Long cardId, String username) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        // Check if user owns the card or is admin
        if (!card.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new ForbiddenException("You don't have permission to view this card");
        }

        return mapToCardDto(card);
    }

    @Transactional(readOnly = true)
    public CardDetailsDto getCardDetails(Long cardId, String username) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!card.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new ForbiddenException("You don't have permission to view this card");
        }

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        BigDecimal todaySpent = transactionRepository.getTodaySpentAmount(
                card, Transaction.TransactionStatus.COMPLETED, startOfDay, startOfNextDay);

        return CardDetailsDto.builder()
                .id(card.getId())
                .maskedCardNumber(card.getMaskedCardNumber())
                .cardHolderName(card.getCardHolderName())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .cardType(card.getCardType())
                .balance(card.getBalance())
                .dailyLimit(card.getDailyLimit())
                .todaySpent(todaySpent != null ? todaySpent : BigDecimal.ZERO)
                .ownerName(card.getOwner().getFullName())
                .ownerEmail(card.getOwner().getEmail())
                .blockReason(card.getBlockReason())
                .blockedAt(card.getBlockedAt())
                .createdAt(card.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<CardDto> getUserCards(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Card> cardPage = cardRepository.findByOwner(user, pageable);
        return mapToPageResponse(cardPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardDto> getAllCards(Pageable pageable) {
        Page<Card> cardPage = cardRepository.findAll(pageable);
        return mapToPageResponse(cardPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardDto> searchUserCards(String username, String searchTerm, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Card> cardPage = cardRepository.searchUserCards(user.getId(), searchTerm, pageable);
        return mapToPageResponse(cardPage);
    }

    @Transactional(readOnly = true)
    public List<CardDto> getActiveUserCards(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Card> activeCards = cardRepository.findActiveCardsByOwner(user, CardStatus.ACTIVE);
        return activeCards.stream()
                .map(this::mapToCardDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardDto updateCard(Long cardId, UpdateCardRequest request, String username) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!isAdmin(username)) {
            throw new ForbiddenException("Only administrators can update cards");
        }

        if (request.getStatus() != null) {
            card.setStatus(request.getStatus());
            if (request.getStatus() == CardStatus.BLOCKED) {
                card.setBlockedAt(LocalDateTime.now());
            }
        }

        if (request.getDailyLimit() != null) {
            card.setDailyLimit(request.getDailyLimit());
        }

        Card updatedCard = cardRepository.save(card);
        log.info("Card {} updated by admin: {}", cardId, username);

        return mapToCardDto(updatedCard);
    }

    @Transactional
    public void requestBlockCard(String username, BlockCardRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!card.getOwner().equals(user)) {
            throw new ForbiddenException("You can only request to block your own cards");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BadRequestException("Card is already blocked");
        }

        // Check if there's already a pending request
        if (blockRequestRepository.existsByCardAndStatus(card, BlockRequest.RequestStatus.PENDING)) {
            throw new ConflictException("There's already a pending block request for this card");
        }

        BlockRequest blockRequest = BlockRequest.builder()
                .card(card)
                .requestedBy(user)
                .reason(request.getReason())
                .status(BlockRequest.RequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        blockRequestRepository.save(blockRequest);
        log.info("Block request created for card {} by user {}", card.getId(), username);
    }

    @Transactional
    public CardDto blockCard(Long cardId, String reason, String adminUsername) {
        if (!isAdmin(adminUsername)) {
            throw new ForbiddenException("Only administrators can block cards");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BadRequestException("Card is already blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        card.setBlockReason(reason);
        card.setBlockedAt(LocalDateTime.now());

        Card blockedCard = cardRepository.save(card);
        log.info("Card {} blocked by admin: {}", cardId, adminUsername);

        return mapToCardDto(blockedCard);
    }

    @Transactional
    public CardDto unblockCard(Long cardId, String adminUsername) {
        if (!isAdmin(adminUsername)) {
            throw new ForbiddenException("Only administrators can unblock cards");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new BadRequestException("Card is not blocked");
        }

        card.setStatus(CardStatus.ACTIVE);
        card.setBlockReason(null);
        card.setBlockedAt(null);

        Card unblockedCard = cardRepository.save(card);
        log.info("Card {} unblocked by admin: {}", cardId, adminUsername);

        return mapToCardDto(unblockedCard);
    }

    @Transactional
    public void deleteCard(Long cardId, String adminUsername) {
        if (!isAdmin(adminUsername)) {
            throw new ForbiddenException("Only administrators can delete cards");
        }

        if (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Card not found");
        }

        cardRepository.deleteById(cardId);
        log.info("Card {} deleted by admin: {}", cardId, adminUsername);
    }

    @Transactional
    public void processBlockRequest(Long requestId, boolean approve, String adminComment, String adminUsername) {
        if (!isAdmin(adminUsername)) {
            throw new ForbiddenException("Only administrators can process block requests");
        }

        BlockRequest blockRequest = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Block request not found"));

        if (blockRequest.getStatus() != BlockRequest.RequestStatus.PENDING) {
            throw new BadRequestException("Block request has already been processed");
        }

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        blockRequest.setStatus(approve ? BlockRequest.RequestStatus.APPROVED : BlockRequest.RequestStatus.REJECTED);
        blockRequest.setProcessedAt(LocalDateTime.now());
        blockRequest.setProcessedBy(admin);
        blockRequest.setAdminComment(adminComment);

        if (approve) {
            Card card = blockRequest.getCard();
            card.setStatus(CardStatus.BLOCKED);
            card.setBlockReason(blockRequest.getReason());
            card.setBlockedAt(LocalDateTime.now());
            cardRepository.save(card);
        }

        blockRequestRepository.save(blockRequest);
        log.info("Block request {} {} by admin: {}", requestId,
                approve ? "approved" : "rejected", adminUsername);
    }

    private boolean isAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ADMIN);
    }

    private CardDto mapToCardDto(Card card) {
        return CardDto.builder()
                .id(card.getId())
                .maskedCardNumber(card.getMaskedCardNumber())
                .cardHolderName(card.getCardHolderName())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .cardType(card.getCardType())
                .balance(card.getBalance())
                .dailyLimit(card.getDailyLimit())
                .ownerUsername(card.getOwner().getUsername())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    private PageResponse<CardDto> mapToPageResponse(Page<Card> cardPage) {
        return PageResponse.<CardDto>builder()
                .content(cardPage.getContent().stream()
                        .map(this::mapToCardDto)
                        .collect(Collectors.toList()))
                .pageNumber(cardPage.getNumber())
                .pageSize(cardPage.getSize())
                .totalElements(cardPage.getTotalElements())
                .totalPages(cardPage.getTotalPages())
                .first(cardPage.isFirst())
                .last(cardPage.isLast())
                .build();
    }
}