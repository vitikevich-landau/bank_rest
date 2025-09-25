package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class TransactionIdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generates a unique transaction ID
     * Format: TXN-YYYYMMDDHHMMSS-XXXX
     */
    public String generateTransactionId() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String uniquePart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("TXN-%s-%s", timestamp, uniquePart);
    }

    /**
     * Generates a unique reference number for transactions
     * Format: REF-XXXXXXXXXX
     */
    public String generateReferenceNumber() {
        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /**
     * Generates a unique block request ID
     * Format: BLK-YYYYMMDD-XXXX
     */
    public String generateBlockRequestId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("BLK-%s-%s", date, uniquePart);
    }
}