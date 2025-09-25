package com.example.bankcards.controller;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.dto.common.ApiResponse;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "Card management APIs")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create card", description = "Create a new card for user (Admin only)")
    public ResponseEntity<ApiResponse<CardDto>> createCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateCardRequest request) {
        CardDto card = cardService.createCard(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Card created successfully", card));
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Get card by ID", description = "Get card information by ID")
    public ResponseEntity<ApiResponse<CardDto>> getCardById(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        CardDto card = cardService.getCardById(cardId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(card));
    }

    @GetMapping("/{cardId}/details")
    @Operation(summary = "Get card details", description = "Get detailed card information")
    public ResponseEntity<ApiResponse<CardDetailsDto>> getCardDetails(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        CardDetailsDto cardDetails = cardService.getCardDetails(cardId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cardDetails));
    }

    @GetMapping("/my-cards")
    @Operation(summary = "Get my cards", description = "Get all cards for current user")
    public ResponseEntity<ApiResponse<PageResponse<CardDto>>> getMyCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<CardDto> cards = cardService.getUserCards(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/my-cards/active")
    @Operation(summary = "Get active cards", description = "Get all active cards for current user")
    public ResponseEntity<ApiResponse<List<CardDto>>> getMyActiveCards(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CardDto> cards = cardService.getActiveUserCards(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/my-cards/search")
    @Operation(summary = "Search my cards", description = "Search current user's cards")
    public ResponseEntity<ApiResponse<PageResponse<CardDto>>> searchMyCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CardDto> cards = cardService.searchUserCards(
                userDetails.getUsername(), searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all cards", description = "Get all cards in the system (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<CardDto>>> getAllCards(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<CardDto> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update card", description = "Update card information (Admin only)")
    public ResponseEntity<ApiResponse<CardDto>> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CardDto card = cardService.updateCard(cardId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Card updated successfully", card));
    }

    @PostMapping("/block-request")
    @Operation(summary = "Request card block", description = "Request to block own card")
    public ResponseEntity<ApiResponse<Void>> requestBlockCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BlockCardRequest request) {
        cardService.requestBlockCard(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Block request submitted successfully", null));
    }

    @PutMapping("/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block card", description = "Block a card immediately (Admin only)")
    public ResponseEntity<ApiResponse<CardDto>> blockCard(
            @PathVariable Long cardId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        CardDto card = cardService.blockCard(cardId, reason, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Card blocked successfully", card));
    }

    @PutMapping("/{cardId}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unblock card", description = "Unblock a card (Admin only)")
    public ResponseEntity<ApiResponse<CardDto>> unblockCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        CardDto card = cardService.unblockCard(cardId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Card unblocked successfully", card));
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete card", description = "Delete a card permanently (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        cardService.deleteCard(cardId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Card deleted successfully", null));
    }

    @PutMapping("/block-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process block request", description = "Approve or reject block request (Admin only)")
    public ResponseEntity<ApiResponse<Void>> processBlockRequest(
            @PathVariable Long requestId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String adminComment,
            @AuthenticationPrincipal UserDetails userDetails) {
        cardService.processBlockRequest(requestId, approve, adminComment, userDetails.getUsername());
        String message = approve ? "Block request approved" : "Block request rejected";
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }
}