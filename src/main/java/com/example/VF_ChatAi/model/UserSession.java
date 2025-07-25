package com.example.VF_ChatAi.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * UserSession model for tracking user authentication sessions
 * This class provides comprehensive session management functionality
 *
 * Features:
 * - Session ID generation and management
 * - User authentication tracking
 * - Session expiration handling
 * - IP address and user agent tracking
 * - Attribute storage system
 * - Session validation methods
 */
public class UserSession {

    private String sessionId;
    private String username;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String ipAddress;
    private String userAgent;
    private boolean isActive;
    private LocalDateTime lastAccessedAt;
    private Map<String, Object> attributes;


    private static final int DEFAULT_SESSION_HOURS = 24;

    /**
     * Default constructor - creates session with auto-generated ID
     */
    public UserSession() {
        this.sessionId = generateSessionId();
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(DEFAULT_SESSION_HOURS);
        this.isActive = true;
        this.attributes = new HashMap<>();
    }

    /**
     * Constructor with basic user details
     */
    public UserSession(String username, Long userId) {
        this();
        this.username = username;
        this.userId = userId;
    }

    /**
     * Constructor compatible with OAuth2Service usage
     * This matches the constructor call pattern found in OAuth2Service
     */
    public UserSession(String sessionId, String username, Long userId,
                       LocalDateTime createdAt, LocalDateTime expiresAt,
                       String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.username = username;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.lastAccessedAt = LocalDateTime.now();
        this.isActive = true;
        this.attributes = new HashMap<>();
    }

    /**
     * Constructor with full details including request info
     */
    public UserSession(String username, Long userId, String ipAddress, String userAgent) {
        this(username, userId);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "sess_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
    }





    /**
     * Check if session is valid (active and not expired)
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }

    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Extend session expiration
     */
    public void extend(int hours) {
        this.expiresAt = LocalDateTime.now().plusHours(hours);
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Extend session by default duration
     */
    public void extend() {
        extend(DEFAULT_SESSION_HOURS);
    }

    /**
     * Invalidate session
     */
    public void invalidate() {
        this.isActive = false;
    }

    /**
     * Update last access time
     */
    public void updateLastAccess() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Refresh session - update last access and extend if needed
     */
    public void refresh() {
        updateLastAccess();

        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now().plusHours(1))) {
            extend();
        }
    }





    /**
     * Set session attribute
     */
    public void setAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(key, value);
    }

    /**
     * Get session attribute
     */
    public Object getAttribute(String key) {
        return this.attributes != null ? this.attributes.get(key) : null;
    }

    /**
     * Get session attribute with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = getAttribute(key);
        return type.isInstance(value) ? (T) value : null;
    }

    /**
     * Remove session attribute
     */
    public void removeAttribute(String key) {
        if (this.attributes != null) {
            this.attributes.remove(key);
        }
    }

    /**
     * Check if attribute exists
     */
    public boolean hasAttribute(String key) {
        return this.attributes != null && this.attributes.containsKey(key);
    }





    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }





    /**
     * Get session duration in minutes
     */
    public long getSessionDurationMinutes() {
        if (createdAt == null) return 0;
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }

    /**
     * Get time until expiration in minutes
     */
    public long getTimeUntilExpirationMinutes() {
        if (expiresAt == null) return -1;
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    /**
     * Check if session is about to expire (within specified minutes)
     */
    public boolean isAboutToExpire(int minutes) {
        return getTimeUntilExpirationMinutes() <= minutes && getTimeUntilExpirationMinutes() > 0;
    }

    /**
     * Create a summary map for JSON serialization
     */
    public Map<String, Object> toSummaryMap() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("sessionId", sessionId);
        summary.put("username", username);
        summary.put("userId", userId);
        summary.put("isActive", isActive);
        summary.put("isValid", isValid());
        summary.put("isExpired", isExpired());
        summary.put("createdAt", createdAt);
        summary.put("expiresAt", expiresAt);
        summary.put("lastAccessedAt", lastAccessedAt);
        summary.put("ipAddress", ipAddress);
        summary.put("sessionDurationMinutes", getSessionDurationMinutes());
        summary.put("timeUntilExpirationMinutes", getTimeUntilExpirationMinutes());
        return summary;
    }





    @Override
    public String toString() {
        return "UserSession{" +
                "sessionId='" + sessionId + '\'' +
                ", username='" + username + '\'' +
                ", userId=" + userId +
                ", isActive=" + isActive +
                ", isValid=" + isValid() +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserSession that = (UserSession) obj;
        return sessionId != null ? sessionId.equals(that.sessionId) : that.sessionId == null;
    }

    @Override
    public int hashCode() {
        return sessionId != null ? sessionId.hashCode() : 0;
    }
}