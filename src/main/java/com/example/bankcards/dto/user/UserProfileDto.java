package com.example.bankcards.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Integer totalCards;
    private Integer activeCards;
    private LocalDateTime memberSince;
}