package com.example.bankcards.controller;

import com.example.bankcards.dto.common.ApiResponse;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.user.*;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Get current user's profile information")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileDto profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Operation(summary = "Get user by ID", description = "Get user information by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {
        UserDto updatedUser = userService.updateUser(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Get all users with pagination (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users", description = "Search users by name or email (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> searchUsers(
            @RequestParam String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserDto> users = userService.searchUsers(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}