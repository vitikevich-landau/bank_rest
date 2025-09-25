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
public class TransactionDto {
    private Long id;
    private String transactionId;
    private String sourceCardMasked;
    private String destinationCardMasked;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private LocalDateTime transactionDate;
    private String referenceNumber;
}