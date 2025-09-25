package com.example.bankcards.controller;

import com.example.bankcards.dto.common.ApiResponse;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "Administrative APIs")
public class AdminController {

    private final UserService userService;

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Toggle user status", description = "Activate or deactivate user account")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean active) {
        userService.toggleUserStatus(userId, active);
        String message = active ? "User activated successfully" : "User deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PutMapping("/users/{userId}/lock")
    @Operation(summary = "Toggle user lock", description = "Lock or unlock user account")
    public ResponseEntity<ApiResponse<Void>> toggleUserLock(
            @PathVariable Long userId,
            @RequestParam boolean locked) {
        userService.toggleUserLock(userId, locked);
        String message = locked ? "User locked successfully" : "User unlocked successfully";
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/users/{userId}/roles")
    @Operation(summary = "Add role to user", description = "Add a new role to user")
    public ResponseEntity<ApiResponse<UserDto>> addRoleToUser(
            @PathVariable Long userId,
            @RequestParam String roleName) {
        UserDto user = userService.addRoleToUser(userId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Role added successfully", user));
    }

    @DeleteMapping("/users/{userId}/roles")
    @Operation(summary = "Remove role from user", description = "Remove a role from user")
    public ResponseEntity<ApiResponse<UserDto>> removeRoleFromUser(
            @PathVariable Long userId,
            @RequestParam String roleName) {
        UserDto user = userService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", user));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Delete user account permanently")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}