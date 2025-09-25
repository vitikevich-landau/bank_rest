package com.example.bankcards.dto.card;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockCardRequest {
    @NotNull(message = "Card ID is required")
    private Long cardId;

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;
}
