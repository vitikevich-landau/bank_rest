package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card.CardType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {
    @NotNull(message = "Card type is required")
    private CardType cardType;

    @NotBlank(message = "Card holder name is required")
    @Size(max = 100)
    private String cardHolderName;

    @DecimalMin(value = "0.0", message = "Daily limit must be positive")
    @DecimalMax(value = "100000.0", message = "Daily limit exceeds maximum")
    private BigDecimal dailyLimit;

    @DecimalMin(value = "0.0", message = "Initial balance must be positive")
    private BigDecimal initialBalance;
}