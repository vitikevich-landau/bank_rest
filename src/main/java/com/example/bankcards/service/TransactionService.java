package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transaction.*;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.*;
import com.example.bankcards.util.TransactionIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final TransactionIdGenerator transactionIdGenerator;

    @Value("${app.card.max-transfer-amount}")
    private BigDecimal maxTransferAmount;

    @Value("${app.card.min-balance}")
    private BigDecimal minBalance;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionDto transfer(String username, TransferRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Card sourceCard = cardRepository.findById(request.getSourceCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found"));

        Card destinationCard = cardRepository.findById(request.getDestinationCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found"));

        // Validate ownership
        if (!sourceCard.getOwner().equals(user)) {
            throw new ForbiddenException("You can only transfer from your own cards");
        }

        if (!destinationCard.getOwner().equals(user)) {
            throw new ForbiddenException("You can only transfer to your own cards");
        }

        // Validate cards are not the same
        if (sourceCard.getId().equals(destinationCard.getId())) {
            throw new BadRequestException("Cannot transfer to the same card");
        }

        // Validate card status
        validateCardForTransaction(sourceCard, "Source");
        validateCardForTransaction(destinationCard, "Destination");

        // Validate amount
        validateTransferAmount(request.getAmount());

        // Check balance
        if (sourceCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        // Check minimum balance
        BigDecimal balanceAfter = sourceCard.getBalance().subtract(request.getAmount());
        if (balanceAfter.compareTo(minBalance) < 0) {
            throw new InsufficientFundsException(
                    "Transfer would result in balance below minimum required: " + minBalance);
        }

        // Check daily limit
        checkDailyLimit(sourceCard, request.getAmount());

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionId(transactionIdGenerator.generateTransactionId())
                .sourceCard(sourceCard)
                .destinationCard(destinationCard)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription() != null ?
                        request.getDescription() : "Transfer between own cards")
                .transactionDate(LocalDateTime.now())
                .referenceNumber(transactionIdGenerator.generateReferenceNumber())
                .balanceBefore(sourceCard.getBalance())
                .build();

        try {
            // Process transfer
            sourceCard.setBalance(sourceCard.getBalance().subtract(request.getAmount()));
            destinationCard.setBalance(destinationCard.getBalance().add(request.getAmount()));

            transaction.setBalanceAfter(sourceCard.getBalance());
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setProcessedDate(LocalDateTime.now());

            cardRepository.save(sourceCard);
            cardRepository.save(destinationCard);
            Transaction savedTransaction = transactionRepository.save(transaction);

            log.info("Transfer completed: {} from card {} to card {}",
                    request.getAmount(), sourceCard.getId(), destinationCard.getId());

            return mapToTransactionDto(savedTransaction);

        } catch (Exception e) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);

            log.error("Transfer failed: {}", e.getMessage());
            throw new InvalidTransactionException("Transfer failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public TransactionDetailsDto getTransactionDetails(Long transactionId, String username) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user is involved in the transaction or is admin
        boolean isOwner = false;
        if (transaction.getSourceCard() != null &&
                transaction.getSourceCard().getOwner().equals(user)) {
            isOwner = true;
        }
        if (transaction.getDestinationCard() != null &&
                transaction.getDestinationCard().getOwner().equals(user)) {
            isOwner = true;
        }

        if (!isOwner && !isAdmin(username)) {
            throw new ForbiddenException("You don't have permission to view this transaction");
        }

        return mapToTransactionDetailsDto(transaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> getUserTransactions(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Transaction> transactionPage = transactionRepository.findByUserId(user.getId(), pageable);
        return mapToPageResponse(transactionPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> getCardTransactions(Long cardId, String username, Pageable pageable) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!card.getOwner().equals(user) && !isAdmin(username)) {
            throw new ForbiddenException("You don't have permission to view transactions for this card");
        }

        Page<Transaction> transactionPage = transactionRepository.findByCard(card, pageable);
        return mapToPageResponse(transactionPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);
        return mapToPageResponse(transactionPage);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = transactionRepository.findTransactionsBetweenDates(startDate, endDate);
        return transactions.stream()
                .map(this::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    private void validateCardForTransaction(Card card, String cardType) {
        if (card.getStatus() == Card.CardStatus.BLOCKED) {
            throw new CardBlockedException(cardType + " card is blocked");
        }
        if (card.getStatus() == Card.CardStatus.EXPIRED) {
            throw new CardExpiredException(cardType + " card has expired");
        }
        if (card.getStatus() != Card.CardStatus.ACTIVE) {
            throw new InvalidTransactionException(cardType + " card is not active");
        }
    }

    private void validateTransferAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transfer amount must be greater than zero");
        }
        if (amount.compareTo(maxTransferAmount) > 0) {
            throw new BadRequestException("Transfer amount exceeds maximum limit: " + maxTransferAmount);
        }
    }

    private void checkDailyLimit(Card card, BigDecimal amount) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        BigDecimal todaySpent = transactionRepository.getTodaySpentAmount(
                card, Transaction.TransactionStatus.COMPLETED, startOfDay, startOfNextDay);

        if (todaySpent == null) {
            todaySpent = BigDecimal.ZERO;
        }

        BigDecimal totalAfterTransfer = todaySpent.add(amount);

        if (card.getDailyLimit() != null && totalAfterTransfer.compareTo(card.getDailyLimit()) > 0) {
            throw new DailyLimitExceededException(
                    "Transfer would exceed daily limit. Today spent: " + todaySpent +
                            ", Daily limit: " + card.getDailyLimit());
        }
    }

    private boolean isAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ADMIN);
    }

    private TransactionDto mapToTransactionDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .sourceCardMasked(transaction.getSourceCard() != null ?
                        transaction.getSourceCard().getMaskedCardNumber() : null)
                .destinationCardMasked(transaction.getDestinationCard() != null ?
                        transaction.getDestinationCard().getMaskedCardNumber() : null)
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .referenceNumber(transaction.getReferenceNumber())
                .build();
    }

    private TransactionDetailsDto mapToTransactionDetailsDto(Transaction transaction) {
        return TransactionDetailsDto.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .referenceNumber(transaction.getReferenceNumber())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .processedDate(transaction.getProcessedDate())
                .sourceCardMasked(transaction.getSourceCard() != null ?
                        transaction.getSourceCard().getMaskedCardNumber() : null)
                .sourceCardHolder(transaction.getSourceCard() != null ?
                        transaction.getSourceCard().getCardHolderName() : null)
                .destinationCardMasked(transaction.getDestinationCard() != null ?
                        transaction.getDestinationCard().getMaskedCardNumber() : null)
                .destinationCardHolder(transaction.getDestinationCard() != null ?
                        transaction.getDestinationCard().getCardHolderName() : null)
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .failureReason(transaction.getFailureReason())
                .build();
    }

    private PageResponse<TransactionDto> mapToPageResponse(Page<Transaction> transactionPage) {
        return PageResponse.<TransactionDto>builder()
                .content(transactionPage.getContent().stream()
                        .map(this::mapToTransactionDto)
                        .collect(Collectors.toList()))
                .pageNumber(transactionPage.getNumber())
                .pageSize(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .first(transactionPage.isFirst())
                .last(transactionPage.isLast())
                .build();
    }
}