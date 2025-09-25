package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Component
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9]{10,15}$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    public boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isValidTransferAmount(BigDecimal amount, BigDecimal maxAmount) {
        return isValidAmount(amount) && amount.compareTo(maxAmount) <= 0;
    }

    public boolean isCardExpired(LocalDate expiryDate) {
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isValidExpiryDate(String expiryDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
            LocalDate date = LocalDate.parse("01/" + expiryDate, DateTimeFormatter.ofPattern("dd/MM/yy"));
            return !isCardExpired(date.withDayOfMonth(date.lengthOfMonth()));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidCVV(String cvv) {
        return cvv != null && cvv.matches("^[0-9]{3,4}$");
    }

    public boolean hasMinimumBalance(BigDecimal balance, BigDecimal minimumBalance) {
        return balance != null && balance.compareTo(minimumBalance) >= 0;
    }

    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // Remove any HTML tags and special characters
        return input.replaceAll("<[^>]*>", "")
                .replaceAll("[<>\"']", "")
                .trim();
    }
}
