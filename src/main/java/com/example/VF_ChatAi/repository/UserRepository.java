package com.example.VF_ChatAi.repository;

import com.example.VF_ChatAi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email (case insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if email exists
     */
    boolean existsByEmailIgnoreCase(String email);
    
    /**
     * Count users by email verification status
     */
    long countByEmailVerifiedTrue();
    
    /**
     * Count enabled users
     */
    long countByEnabledTrue();
    
    /**
     * Find all verified users
     */
    List<User> findByEmailVerifiedTrue();
    
    /**
     * Find all enabled users
     */
    List<User> findByEnabledTrue();
    
    /**
     * Find all active users (enabled and verified)
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.emailVerified = true")
    List<User> findActiveUsers();
    
    /**
     * Count active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.emailVerified = true")
    long countActiveUsers();
}
