package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequest {
    private Card.CardStatus status;

    @DecimalMin(value = "0.0", message = "Daily limit must be positive")
    @DecimalMax(value = "100000.0", message = "Daily limit exceeds maximum")
    private BigDecimal dailyLimit;
}
