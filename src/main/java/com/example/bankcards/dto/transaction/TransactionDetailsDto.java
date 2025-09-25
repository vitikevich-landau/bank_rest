package com.example.bankcards.dto.transaction;

import com.example.bankcards.entity.Transaction.TransactionStatus;
import com.example.bankcards.entity.Transaction.TransactionType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailsDto {
    private Long id;
    private String transactionId;
    private String referenceNumber;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private LocalDateTime processedDate;
    private String sourceCardMasked;
    private String sourceCardHolder;
    private String destinationCardMasked;
    private String destinationCardHolder;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String failureReason;
}