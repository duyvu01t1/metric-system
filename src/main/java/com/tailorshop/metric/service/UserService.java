package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.ChangePasswordRequest;
import com.tailorshop.metric.dto.SignUpRequest;
import com.tailorshop.metric.dto.UserDTO;
import com.tailorshop.metric.dto.UserProfileUpdateRequest;
import com.tailorshop.metric.dto.UserRoleDTO;
import com.tailorshop.metric.entity.User;
import com.tailorshop.metric.entity.UserRole;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.repository.UserRepository;
import com.tailorshop.metric.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     */
    @Transactional
    public UserDTO signUp(SignUpRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setOauthProvider("LOCAL");

        // Assign default USER role
        UserRole userRole = userRoleRepository.findByName("USER")
            .orElseThrow(() -> new BusinessException("Default USER role not found"));
        
        Set<UserRole> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Save user
        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());

        return convertToDTO(savedUser);
    }

    /**
     * List all users (id + username + email) — dùng cho dropdown chọn tài khoản
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get user by username
     */
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("User not found"));
        return convertToDTO(user);
    }

    /**
     * Get user by email
     */
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("User not found"));
        return convertToDTO(user);
    }

    /**
     * Get full profile of the currently authenticated user (includes roles)
     */
    public UserDTO getCurrentProfile(String username) {
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new BusinessException("User not found: " + username));
        return convertToDTOWithRoles(user);
    }

    /**
     * Update firstName, lastName, email, phone and avatarUrl for the current user
     */
    @Transactional
    public UserDTO updateProfile(String username, UserProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found: " + username));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim().isEmpty() ? null : request.getAvatarUrl().trim());
        }
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmail(newEmail)) {
                throw new BusinessException("Email is already in use by another account");
            }
            user.setEmail(newEmail);
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for user: {}", username);
        return convertToDTO(saved);
    }

    /**
     * Change the authenticated user's password.
     * For OAuth users (non-LOCAL) the currentPassword check is skipped.
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found: " + username));

        if ("LOCAL".equalsIgnoreCase(user.getOauthProvider())) {
            if (user.getPasswordHash() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessException("Current password is incorrect");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", username);
    }

    /**
     * Convert User to UserDTO (without roles)
     */
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .avatarUrl(user.getAvatarUrl())
            .oauthProvider(user.getOauthProvider())
            .isActive(user.getIsActive())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .roles(null)
            .build();
    }

    /**
     * Convert User to UserDTO including roles
     */
    private UserDTO convertToDTOWithRoles(User user) {
        Set<UserRoleDTO> roleDTOs = user.getRoles() == null ? new HashSet<>() :
                user.getRoles().stream()
                        .map(r -> UserRoleDTO.builder()
                                .id(r.getId())
                                .name(r.getName())
                                .description(r.getDescription())
                                .build())
                        .collect(Collectors.toSet());

        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .avatarUrl(user.getAvatarUrl())
            .oauthProvider(user.getOauthProvider())
            .isActive(user.getIsActive())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .roles(roleDTOs)
            .build();
    }
}
