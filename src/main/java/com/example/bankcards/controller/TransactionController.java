package com.example.bankcards.controller;

import com.example.bankcards.dto.common.ApiResponse;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transaction.*;
import com.example.bankcards.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "Transaction management APIs")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money", description = "Transfer money between own cards")
    public ResponseEntity<ApiResponse<TransactionDto>> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {
        TransactionDto transaction = transactionService.transfer(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer completed successfully", transaction));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details", description = "Get transaction details by ID")
    public ResponseEntity<ApiResponse<TransactionDetailsDto>> getTransactionDetails(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        TransactionDetailsDto details = transactionService.getTransactionDetails(
                transactionId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @GetMapping("/my-transactions")
    @Operation(summary = "Get my transactions", description = "Get all transactions for current user")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto>>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TransactionDto> transactions = transactionService.getUserTransactions(
                userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/card/{cardId}")
    @Operation(summary = "Get card transactions", description = "Get all transactions for a specific card")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto>>> getCardTransactions(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TransactionDto> transactions = transactionService.getCardTransactions(
                cardId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all transactions", description = "Get all transactions (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto>>> getAllTransactions(
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TransactionDto> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get transactions by date range", description = "Get transactions within date range (Admin only)")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<TransactionDto> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}