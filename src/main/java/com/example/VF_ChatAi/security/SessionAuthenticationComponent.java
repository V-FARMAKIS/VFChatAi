package com.example.VF_ChatAi.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Custom component to handle session-based authentication
 * This bridges the gap between Spring Security and our custom session management
 */
@Component
public class SessionAuthenticationComponent {

    /**
     * Check if current request has valid authentication
     */
    public boolean isAuthenticated() {
        return getCurrentSession()
                .map(this::isValidSession)
                .orElse(false);
    }

    /**
     * Get current user email from session
     */
    public Optional<String> getCurrentUserEmail() {
        return getCurrentSession()
                .filter(this::isValidSession)
                .map(session -> (String) session.getAttribute("userEmail"));
    }

    /**
     * Get current user ID from session
     */
    public Optional<Long> getCurrentUserId() {
        return getCurrentSession()
                .filter(this::isValidSession)
                .map(session -> (Long) session.getAttribute("userId"));
    }

    /**
     * Get current HTTP session if available
     */
    private Optional<HttpSession> getCurrentSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false); // Don't create new session
        return Optional.ofNullable(session);
    }

    /**
     * Check if session is valid and has required authentication attributes
     */
    private boolean isValidSession(HttpSession session) {
        if (session == null) {
            return false;
        }


        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return false;
        }


        try {
            long loginTime = Optional.ofNullable((Long) session.getAttribute("loginTime"))
                    .orElse(0L);
            long sessionAge = System.currentTimeMillis() - loginTime;
            long maxAge = 8 * 60 * 60 * 1000; // 8 hours

            return sessionAge <= maxAge;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create authenticated session for user
     */
    public void createAuthenticatedSession(String userEmail, Long userId, String userName, String ipAddress) {
        getCurrentSession().ifPresent(session -> {

            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {

            }
        });


        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            HttpSession newSession = request.getSession(true);


            newSession.setAttribute("userId", userId);
            newSession.setAttribute("userEmail", userEmail);
            newSession.setAttribute("userName", userName);
            newSession.setAttribute("loginTime", System.currentTimeMillis());
            newSession.setAttribute("ipAddress", ipAddress);


            newSession.setMaxInactiveInterval(8 * 60 * 60);
        }
    }

    /**
     * Invalidate current session
     */
    public void invalidateSession() {
        getCurrentSession().ifPresent(session -> {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {

            }
        });
    }

    /**
     * Update session last access time
     */
    public void updateLastAccess() {
        getCurrentSession().ifPresent(session -> {
            session.setAttribute("lastAccess", System.currentTimeMillis());
        });
    }
}