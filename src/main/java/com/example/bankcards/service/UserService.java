package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToUserDto(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long totalCards = cardRepository.countByOwnerAndStatus(user, null);
        long activeCards = cardRepository.countByOwnerAndStatus(user, Card.CardStatus.ACTIVE);

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .totalCards((int) totalCards)
                .activeCards((int) activeCards)
                .memberSince(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserDto updateUser(String username, UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email is already in use");
            }
            user.setEmail(request.getEmail());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated: {}", username);

        return mapToUserDto(updatedUser);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return mapToPageResponse(userPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> searchUsers(String searchTerm, Pageable pageable) {
        Page<User> userPage = userRepository.searchUsers(searchTerm, pageable);
        return mapToPageResponse(userPage);
    }

    @Transactional
    public void toggleUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(active);
        userRepository.save(user);

        log.info("User {} status changed to: {}", user.getUsername(), active ? "active" : "inactive");
    }

    @Transactional
    public void toggleUserLock(Long userId, boolean locked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLocked(locked);
        userRepository.save(user);

        log.info("User {} lock status changed to: {}", user.getUsername(), locked ? "locked" : "unlocked");
    }

    @Transactional
    public UserDto addRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);
        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        user.getRoles().add(role);
        User updatedUser = userRepository.save(user);

        log.info("Role {} added to user {}", roleName, user.getUsername());
        return mapToUserDto(updatedUser);
    }

    @Transactional
    public UserDto removeRoleFromUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);
        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        user.getRoles().remove(role);
        User updatedUser = userRepository.save(user);

        log.info("Role {} removed from user {}", roleName, user.getUsername());
        return mapToUserDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        userRepository.deleteById(userId);
        log.info("User deleted with id: {}", userId);
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .locked(user.isLocked())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().toString())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private PageResponse<UserDto> mapToPageResponse(Page<User> userPage) {
        return PageResponse.<UserDto>builder()
                .content(userPage.getContent().stream()
                        .map(this::mapToUserDto)
                        .collect(Collectors.toList()))
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }
}