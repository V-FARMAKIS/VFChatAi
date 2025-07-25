package com.example.VF_ChatAi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Database maintenance controller
 * Should be removed or secured in production
 */
@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Fix password column size to accommodate BCrypt hashes
     */
    @PostMapping("/fix-password-column")
    public Map<String, Object> fixPasswordColumn() {
        try {

            String checkSql = "SELECT character_maximum_length FROM information_schema.columns " +
                             "WHERE table_name = 'users' AND column_name = 'password'";
            Integer currentSize = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (currentSize != null && currentSize < 255) {

                jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN password TYPE VARCHAR(255)");
                return Map.of(
                    "success", true, 
                    "message", "Password column updated from VARCHAR(" + currentSize + ") to VARCHAR(255)",
                    "previousSize", currentSize,
                    "newSize", 255
                );
            } else {
                return Map.of(
                    "success", true, 
                    "message", "Password column already has sufficient size",
                    "currentSize", currentSize != null ? currentSize : "unlimited"
                );
            }
        } catch (Exception e) {
            return Map.of(
                "success", false, 
                "error", e.getMessage(),
                "suggestion", "Run the fix-password-column.sql script manually"
            );
        }
    }

    /**
     * Health check that includes database column validation
     */
    @GetMapping("/health-check")
    public Map<String, Object> healthCheck() {
        try {

            String sql = "SELECT character_maximum_length FROM information_schema.columns " +
                        "WHERE table_name = 'users' AND column_name = 'password'";
            Integer passwordColumnSize = jdbcTemplate.queryForObject(sql, Integer.class);
            

            String countSql = "SELECT COUNT(*) FROM users WHERE password IS NOT NULL AND LENGTH(password) >= 60";
            Integer validPasswordCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            

            Integer totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            
            return Map.of(
                "status", "OK",
                "database", Map.of(
                    "passwordColumnSize", passwordColumnSize != null ? passwordColumnSize : "unlimited",
                    "passwordColumnOK", passwordColumnSize == null || passwordColumnSize >= 255,
                    "totalUsers", totalUsers,
                    "usersWithValidPasswords", validPasswordCount
                ),
                "recommendation", passwordColumnSize != null && passwordColumnSize < 255 ? 
                    "Run POST /api/maintenance/fix-password-column to fix password storage" : 
                    "Database schema is properly configured"
            );
        } catch (Exception e) {
            return Map.of(
                "status", "ERROR",
                "error", e.getMessage()
            );
        }
    }
}
