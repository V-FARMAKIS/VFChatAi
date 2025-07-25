package com.example.VF_ChatAi.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.VF_ChatAi.model.User;
import com.example.VF_ChatAi.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }

    /**
     * Save user
     */
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        

        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        log.info("User saved: {}", savedUser.getEmail());
        return savedUser;
    }

    /**
     * Create new user
     */
    public User createUser(String email, String rawPassword, String firstName, String lastName) {

        if (findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }


        User user = new User();
        user.setEmail(email.toLowerCase().trim());
        
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName != null ? lastName.trim() : "");
        user.setEmailVerified(false);
        user.setEnabled(false);

        return save(user);
    }

    /**
     * Update user password
     */
    public void updatePassword(User user, String newPassword) {
        if (user == null || newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Invalid user or password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        save(user);
        log.info("Password updated for user: {}", user.getEmail());
    }

    /**
     * Verify user email
     */
    public void verifyEmail(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        user.setEmailVerified(true);
        user.setEnabled(true);
        save(user);
        log.info("Email verified for user: {}", user.getEmail());
    }

    /**
     * Enable user account
     */
    public void enableUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        user.setEnabled(true);
        save(user);
        log.info("User enabled: {}", user.getEmail());
    }

    /**
     * Disable user account
     */
    public void disableUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        user.setEnabled(false);
        save(user);
        log.info("User disabled: {}", user.getEmail());
    }

    /**
     * Check if password matches
     */
    public boolean isPasswordValid(User user, String rawPassword) {
        if (user == null || rawPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * Get all users (for admin purposes)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Count total users
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * Count verified users
     */
    public long countVerifiedUsers() {
        return userRepository.countByEmailVerifiedTrue();
    }

    /**
     * Count enabled users
     */
    public long countEnabledUsers() {
        return userRepository.countByEnabledTrue();
    }

    /**
     * Delete user (for admin purposes)
     */
    public void deleteUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Optional<User> userOpt = findById(userId);
        if (userOpt.isPresent()) {
            userRepository.deleteById(userId);
            log.info("User deleted: {}", userOpt.get().getEmail());
        } else {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
    }

    /**
     * Update user profile
     */
    public User updateProfile(User user, String firstName, String lastName) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName.trim());
        }

        if (lastName != null) {
            user.setLastName(lastName.trim());
        }

        return save(user);
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return findByEmail(email).isPresent();
    }
}
