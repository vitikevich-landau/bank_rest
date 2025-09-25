package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card.CardStatus;
import com.example.bankcards.entity.Card.CardType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailsDto {
    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private LocalDate expiryDate;
    private CardStatus status;
    private CardType cardType;
    private BigDecimal balance;
    private BigDecimal dailyLimit;
    private BigDecimal todaySpent;
    private String ownerName;
    private String ownerEmail;
    private String blockReason;
    private LocalDateTime blockedAt;
    private LocalDateTime createdAt;
}