package com.example.bankcards.dto.transaction;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotNull(message = "Source card ID is required")
    private Long sourceCardId;

    @NotNull(message = "Destination card ID is required")
    private Long destinationCardId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "50000.00", message = "Amount exceeds maximum transfer limit")
    private BigDecimal amount;

    @Size(max = 500)
    private String description;
}