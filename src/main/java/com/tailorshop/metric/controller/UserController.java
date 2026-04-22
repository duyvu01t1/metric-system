package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.ChangePasswordRequest;
import com.tailorshop.metric.dto.UserDTO;
import com.tailorshop.metric.dto.UserProfileUpdateRequest;
import com.tailorshop.metric.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller — profile view and edit for the authenticated user
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;

    /**
     * Get current logged-in user profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(Authentication authentication) {
        log.debug("GET /users/me — user={}", authentication.getName());
        UserDTO userDTO = userService.getCurrentProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", userDTO));
    }

    /**
     * Update current user profile (firstName, lastName, email, phone, avatarUrl)
     */
    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        log.info("PUT /users/me — user={}", authentication.getName());
        UserDTO updated = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    /**
     * Change password for the current user
     */
    @PostMapping("/me/change-password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<?>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("POST /users/me/change-password — user={}", authentication.getName());
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
