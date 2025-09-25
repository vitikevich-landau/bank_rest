package com.example.bankcards.dto.common;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private int status;
    private String error;
    private Map<String, String> fieldErrors;
    private Long timestamp;
}