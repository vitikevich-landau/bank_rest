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
public class TransactionFilterRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private TransactionType type;
    private TransactionStatus status;
    private Long cardId;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}