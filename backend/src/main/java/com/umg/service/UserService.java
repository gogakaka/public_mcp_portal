package com.umg.service;

import com.umg.domain.entity.User;
import com.umg.domain.enums.UserRole;
import com.umg.dto.PageResponse;
import com.umg.dto.UserDto;
import com.umg.exception.DuplicateResourceException;
import com.umg.exception.ResourceNotFoundException;
import com.umg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service handling user CRUD operations, registration, and profile management.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user in the system.
     *
     * @param request the user creation request
     * @return the created user response
     * @throws DuplicateResourceException if the email is already registered
     */
    @Transactional
    public UserDto.Response register(UserDto.CreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .name(request.getName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .department(request.getDepartment())
                .role(UserRole.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {} ({})", saved.getEmail(), saved.getId());
        return toResponse(saved);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the user's UUID
     * @return the user response
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public UserDto.Response getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return toResponse(user);
    }

    /**
     * Retrieves a user entity by their ID (for internal use).
     *
     * @param userId the user's UUID
     * @return the User entity
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    /**
     * Updates the profile of the specified user.
     *
     * @param userId  the user's UUID
     * @param request the update request
     * @return the updated user response
     */
    @Transactional
    public UserDto.Response updateProfile(UUID userId, UserDto.UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        User updated = userRepository.save(user);
        log.info("Updated profile for user: {}", updated.getId());
        return toResponse(updated);
    }

    /**
     * Lists all users with pagination (admin only).
     *
     * @param pageable pagination information
     * @return a page of user responses
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDto.Response> listUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Converts a User entity to a Response DTO.
     *
     * @param user the entity
     * @return the DTO
     */
    private UserDto.Response toResponse(User user) {
        return UserDto.Response.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .department(user.getDepartment())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
