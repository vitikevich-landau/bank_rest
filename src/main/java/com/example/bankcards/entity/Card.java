package com.example.bankcards.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_number", columnList = "encrypted_card_number"),
        @Index(name = "idx_card_owner", columnList = "owner_id"),
        @Index(name = "idx_card_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card extends BaseEntity {

    @Column(name = "encrypted_card_number", nullable = false, unique = true)
    private String encryptedCardNumber;

    @Column(name = "masked_card_number", nullable = false, length = 19)
    private String maskedCardNumber; // **** **** **** 1234

    @Column(name = "card_holder_name", nullable = false, length = 100)
    private String cardHolderName;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "cvv", nullable = false, length = 255)
    private String encryptedCvv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "daily_limit", precision = 15, scale = 2)
    private BigDecimal dailyLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "sourceCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> outgoingTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "destinationCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> incomingTransactions = new ArrayList<>();

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    public enum CardStatus {
        ACTIVE,
        BLOCKED,
        EXPIRED,
        PENDING_ACTIVATION
    }

    public enum CardType {
        DEBIT,
        CREDIT,
        VIRTUAL
    }

    @PrePersist
    @PreUpdate
    public void checkExpiry() {
        if (LocalDate.now().isAfter(expiryDate) && status != CardStatus.EXPIRED) {
            status = CardStatus.EXPIRED;
        }
    }
}