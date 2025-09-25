package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CardNumberUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Masks a card number showing only last 4 digits
     * Example: 1234567890123456 -> **** **** **** 3456
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        String cleanNumber = cardNumber.replaceAll("[^0-9]", "");
        int length = cleanNumber.length();

        if (length < 4) {
            return "****";
        }

        String lastFour = cleanNumber.substring(length - 4);
        return "**** **** **** " + lastFour;
    }

    /**
     * Generates a random 16-digit card number (for demonstration)
     * In production, this would follow proper card number generation rules
     */
    public String generateCardNumber() {
        StringBuilder cardNumber = new StringBuilder();
        // Start with 4 for Visa or 5 for Mastercard
        cardNumber.append(RANDOM.nextInt(2) == 0 ? "4" : "5");

        // Generate remaining 15 digits
        for (int i = 0; i < 15; i++) {
            cardNumber.append(RANDOM.nextInt(10));
        }

        return cardNumber.toString();
    }

    /**
     * Generates a 3-digit CVV
     */
    public String generateCVV() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    /**
     * Validates card number format (basic validation)
     */
    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        String cleanNumber = cardNumber.replaceAll("[^0-9]", "");

        // Check if it's 13-19 digits (standard card length)
        if (cleanNumber.length() < 13 || cleanNumber.length() > 19) {
            return false;
        }

        // Luhn algorithm validation
        return isValidLuhn(cleanNumber);
    }

    /**
     * Luhn algorithm implementation for card validation
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Formats card number with spaces for display
     * Example: 1234567890123456 -> 1234 5678 9012 3456
     */
    public String formatCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return "";
        }

        String cleanNumber = cardNumber.replaceAll("[^0-9]", "");

        if (cleanNumber.length() != 16) {
            return cleanNumber;
        }

        return String.format("%s %s %s %s",
                cleanNumber.substring(0, 4),
                cleanNumber.substring(4, 8),
                cleanNumber.substring(8, 12),
                cleanNumber.substring(12, 16));
    }
}