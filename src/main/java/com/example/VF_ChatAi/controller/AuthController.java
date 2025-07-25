package com.example.VF_ChatAi.controller;

import com.example.VF_ChatAi.model.User;
import com.example.VF_ChatAi.security.SessionAuthenticationComponent;
import com.example.VF_ChatAi.service.EmailService;
import com.example.VF_ChatAi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private SessionAuthenticationComponent sessionAuth;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    private final ConcurrentHashMap<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();

    private static class PasswordResetToken {
        String email;
        LocalDateTime expiryTime;

        PasswordResetToken(String email) {
            this.email = email;
            this.expiryTime = LocalDateTime.now().plus(1, ChronoUnit.HOURS);
        }
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        try {
            String emailParam = (String) requestBody.get("email");
            String passwordParam = (String) requestBody.get("password");
            String confirmPasswordParam = (String) requestBody.get("confirmPassword");
            String firstNameParam = (String) requestBody.get("firstName");
            String lastNameParam = (String) requestBody.get("lastName");


            if (firstNameParam == null || firstNameParam.trim().isEmpty()) {
                firstNameParam = emailParam.split("@")[0];
            }


            if (emailParam == null || emailParam.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email is required", "VALIDATION_ERROR"));
            }

            if (passwordParam == null || passwordParam.length() < 8) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Password must be at least 8 characters", "VALIDATION_ERROR"));
            }

            if (!isPasswordStrong(passwordParam)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character", "WEAK_PASSWORD"));
            }

            if (confirmPasswordParam == null || !passwordParam.equals(confirmPasswordParam)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Passwords do not match", "VALIDATION_ERROR"));
            }


            if (userService.findByEmail(emailParam).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email already registered", "EMAIL_EXISTS"));
            }


            log.info("Creating new user account for email: {}", emailParam);
            User savedUser = userService.createUser(
                    emailParam.toLowerCase().trim(),
                    passwordParam,
                    firstNameParam.trim(),
                    lastNameParam != null ? lastNameParam.trim() : ""
            );
            log.info("User account created successfully with ID: {}", savedUser.getId());


            boolean emailSent = emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (emailSent) {
                response.put("message", "Registration successful! Please check your email for the verification code.");
            } else {
                response.put("message", "Registration successful! Email service is currently unavailable, but your account has been created.");
                response.put("emailError", true);
                log.warn("Email not sent during registration for user: {}", savedUser.getEmail());
            }

            response.put("email", savedUser.getEmail());
            response.put("requiresVerification", true);
            response.put("emailSent", emailSent);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Email verification
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String code = (String) request.get("code");

            if (email == null || code == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email and code are required", "VALIDATION_ERROR"));
            }


            boolean codeValid = emailService.verifyCode(email, code);
            if (!codeValid) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid or expired verification code", "INVALID_CODE"));
            }


            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("User not found", "USER_NOT_FOUND"));
            }

            User user = userOpt.get();
            user.setEmailVerified(true);
            user.setEnabled(true);
            userService.save(user);


            String userName = user.getFirstName() + " " + user.getLastName();
            emailService.sendWelcomeEmail(user.getEmail(), userName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verified successfully");
            response.put("redirectUrl", "/account-success.html");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Email verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Verification failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Resend verification code
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email is required", "VALIDATION_ERROR"));
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("User not found", "USER_NOT_FOUND"));
            }

            User user = userOpt.get();
            if (user.isEmailVerified()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email already verified", "ALREADY_VERIFIED"));
            }

            String userName = user.getFirstName() + " " + user.getLastName();
            boolean emailSent = emailService.resendVerificationCode(user.getEmail(), userName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification code resent");
            response.put("emailSent", emailSent);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Resend verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to resend verification", "INTERNAL_ERROR"));
        }
    }



    /**
     * User login with proper session management
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        try {
            String emailParam = (String) requestBody.get("email");
            String passwordParam = (String) requestBody.get("password");

            if (emailParam == null || passwordParam == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email and password are required", "VALIDATION_ERROR"));
            }

            Optional<User> userOpt = userService.findByEmail(emailParam.toLowerCase().trim());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid email or password", "INVALID_CREDENTIALS"));
            }

            User user = userOpt.get();

            if (!passwordEncoder.matches(passwordParam, user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid email or password", "INVALID_CREDENTIALS"));
            }


            if (!user.isEmailVerified()) {
                emailService.sendVerificationEmail(user.getEmail(), user.getFirstName());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Email not verified. We've sent a new verification code to your email.",
                        "errorCode", "EMAIL_NOT_VERIFIED",
                        "email", user.getEmail(),
                        "requiresVerification", true
                ));
            }

            if (!user.isEnabled()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Account is disabled", "ACCOUNT_DISABLED"));
            }


            String clientIP = getClientIP(request);
            String fullName = user.getFirstName() + " " + user.getLastName();

            sessionAuth.createAuthenticatedSession(
                    user.getEmail(),
                    user.getId(),
                    fullName,
                    clientIP
            );

            log.info("User logged in successfully: {} from IP: {}", user.getEmail(), clientIP);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("redirectUrl", "/chat.html");
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "emailVerified", user.isEmailVerified()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Login failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * User logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        try {
            String userEmail = (String) session.getAttribute("userEmail");
            if (userEmail != null) {
                log.info("User logged out: {}", userEmail);
            }

            session.invalidate();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Logout failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get current user session
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getSession(HttpSession session) {
        try {
            String userEmail = (String) session.getAttribute("userEmail");

            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("No active session", "UNAUTHORIZED"));
            }

            Optional<User> userOpt = userService.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                session.invalidate();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not found", "USER_NOT_FOUND"));
            }

            User user = userOpt.get();


            Long loginTime = (Long) session.getAttribute("loginTime");
            if (loginTime != null) {
                long sessionAge = System.currentTimeMillis() - loginTime;
                long maxAge = 8 * 60 * 60 * 1000; // 8 hours in milliseconds

                if (sessionAge > maxAge) {
                    session.invalidate();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(createErrorResponse("Session expired", "SESSION_EXPIRED"));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "emailVerified", user.isEmailVerified()
            ));
            response.put("sessionInfo", Map.of(
                    "sessionId", session.getId(),
                    "loginTime", loginTime,
                    "maxInactiveInterval", session.getMaxInactiveInterval()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Session check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Session check failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Password reset request
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email is required", "VALIDATION_ERROR"));
            }

            Optional<User> userOpt = userService.findByEmail(email.toLowerCase().trim());
            if (userOpt.isEmpty()) {

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "If the email exists, a reset link has been sent");
                return ResponseEntity.ok(response);
            }

            User user = userOpt.get();
            String resetToken = UUID.randomUUID().toString();


            resetTokens.put(resetToken, new PasswordResetToken(user.getEmail()));


            String userName = user.getFirstName() + " " + user.getLastName();
            emailService.sendPasswordResetEmail(user.getEmail(), userName, resetToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If the email exists, a reset link has been sent");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Password reset request failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Password reset request failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Reset password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> request) {
        try {
            String token = (String) request.get("token");
            String newPassword = (String) request.get("password");

            if (token == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Token and password are required", "VALIDATION_ERROR"));
            }

            if (newPassword.length() < 8) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Password must be at least 8 characters", "VALIDATION_ERROR"));
            }

            if (!isPasswordStrong(newPassword)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character", "WEAK_PASSWORD"));
            }

            PasswordResetToken resetToken = resetTokens.get(token);
            if (resetToken == null || LocalDateTime.now().isAfter(resetToken.expiryTime)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid or expired reset token", "INVALID_TOKEN"));
            }

            Optional<User> userOpt = userService.findByEmail(resetToken.email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("User not found", "USER_NOT_FOUND"));
            }

            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.save(user);


            resetTokens.remove(token);

            log.info("Password reset successful for user: {}", user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Password reset failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "VFChat Auth Service");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }



    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("errorCode", errorCode);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}