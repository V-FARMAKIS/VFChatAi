package com.example.VF_ChatAi.service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Transactional
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from-email}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${twofa.code.length:6}")
    private int codeLength;

    @Value("${twofa.code.expiry-minutes:15}")
    private int expiryMinutes;


    private final ConcurrentHashMap<String, VerificationCode> verificationCodes = new ConcurrentHashMap<>();

    private static class VerificationCode {
        String code;
        LocalDateTime expiryTime;
        int attempts;

        VerificationCode(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.attempts = 0;
        }
    }

    /**
     * Send verification email with 2FA code
     */
    public boolean sendVerificationEmail(String toEmail, String userName) {
        try {
            String verificationCode = generateVerificationCode();
            

            VerificationCode codeData = new VerificationCode(
                verificationCode, 
                LocalDateTime.now().plus(expiryMinutes, ChronoUnit.MINUTES)
            );
            verificationCodes.put(toEmail, codeData);
            log.info("Generated verification code for {}: {}", toEmail, verificationCode);

            String subject = "VFChatAI - Email Verification Code";
            String htmlContent = buildVerificationEmailTemplate(userName, verificationCode);

            boolean emailSent = sendHtmlEmail(toEmail, subject, htmlContent);
            
            if (!emailSent) {
                log.warn("Email not sent to {} - likely due to SMTP configuration issues. Code is still valid: {}", toEmail, verificationCode);
            }
            
            return emailSent;

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send password reset email
     */
    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            String subject = "VFChatAI - Password Reset Request";
            String htmlContent = buildPasswordResetTemplate(userName, resetToken);

            return sendHtmlEmail(toEmail, subject, htmlContent);

        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send welcome email
     */
    public boolean sendWelcomeEmail(String toEmail, String userName) {
        try {
            String subject = "Welcome to VFChatAI!";
            String htmlContent = buildWelcomeEmailTemplate(userName);

            return sendHtmlEmail(toEmail, subject, htmlContent);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verify the 2FA code
     */
    public boolean verifyCode(String email, String code) {
        VerificationCode storedCode = verificationCodes.get(email);
        
        if (storedCode == null) {
            log.warn("No verification code found for email: {}", email);
            return false;
        }

        if (LocalDateTime.now().isAfter(storedCode.expiryTime)) {
            log.warn("Verification code expired for email: {}", email);
            verificationCodes.remove(email);
            return false;
        }

        storedCode.attempts++;
        
        if (storedCode.attempts > 3) {
            log.warn("Too many verification attempts for email: {}", email);
            verificationCodes.remove(email);
            return false;
        }

        if (storedCode.code.equals(code)) {
            verificationCodes.remove(email);
            log.info("Email verification successful for: {}", email);
            return true;
        } else {
            log.warn("Invalid verification code for email: {}", email);
            return false;
        }
    }

    /**
     * Resend verification code
     */
    public boolean resendVerificationCode(String email, String userName) {

        verificationCodes.remove(email);
        

        return sendVerificationEmail(email, userName);
    }

    /**
     * Get stored verification code for development purposes
     */
    public String getStoredVerificationCode(String email) {
        VerificationCode storedCode = verificationCodes.get(email);
        if (storedCode == null) {
            return null;
        }
        
        if (LocalDateTime.now().isAfter(storedCode.expiryTime)) {
            verificationCodes.remove(email);
            return null;
        }
        
        return storedCode.code;
    }



    private boolean sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {

            if (fromEmail == null || fromEmail.trim().isEmpty()) {
                log.warn("Email configuration not available. SMTP_USERNAME not set. Email not sent to: {}", toEmail);
                return false;
            }
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", toEmail);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding error when sending email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        
        return code.toString();
    }

    private String buildVerificationEmailTemplate(String userName, String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Email Verification</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 28px; font-weight: bold; color: #4A90E2; margin-bottom: 10px; }
                    .code-container { background-color: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }
                    .verification-code { font-size: 32px; font-weight: bold; color: #4A90E2; letter-spacing: 5px; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">VFChatAI</div>
                        <h2>Email Verification Required</h2>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>Thank you for registering with VFChatAI! To complete your account setup, please verify your email address using the code below:</p>
                    
                    <div class="code-container">
                        <div class="verification-code">%s</div>
                    </div>
                    
                    <p><strong>Important:</strong></p>
                    <ul>
                        <li>This code will expire in %d minutes</li>
                        <li>Enter this code exactly as shown</li>
                        <li>Do not share this code with anyone</li>
                    </ul>
                    
                    <p>If you didn't create an account with VFChatAI, please ignore this email.</p>
                    
                    <div class="footer">
                        <p>This is an automated message from VFChatAI. Please do not reply to this email.</p>
                        <p>&copy; 2025 VFChatAI. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, code, expiryMinutes);
    }

    private String buildPasswordResetTemplate(String userName, String resetToken) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 28px; font-weight: bold; color: #4A90E2; margin-bottom: 10px; }
                    .reset-button { display: inline-block; background-color: #4A90E2; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">VFChatAI</div>
                        <h2>Password Reset Request</h2>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>We received a request to reset your password for your VFChatAI account. Click the button below to reset your password:</p>
                    
                    <div style="text-align: center;">
                        <a href="http://localhost:8080/reset-password.html?token=%s" class="reset-button">Reset Password</a>
                    </div>
                    
                    <p><strong>Important:</strong></p>
                    <ul>
                        <li>This link will expire in 1 hour</li>
                        <li>If you didn't request this reset, please ignore this email</li>
                        <li>For security, this link can only be used once</li>
                    </ul>
                    
                    <div class="footer">
                        <p>This is an automated message from VFChatAI. Please do not reply to this email.</p>
                        <p>&copy; 2025 VFChatAI. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetToken);
    }

    private String buildWelcomeEmailTemplate(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to VFChatAI</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 28px; font-weight: bold; color: #4A90E2; margin-bottom: 10px; }
                    .feature { margin: 15px 0; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">VFChatAI</div>
                        <h2>Welcome to VFChatAI!</h2>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>Welcome to VFChatAI! Your account has been successfully created and verified. You now have access to our advanced AI-powered chat platform.</p>
                    
                    <h3>What you can do with VFChatAI:</h3>
                    
                    <div class="feature">
                        <strong>🤖 AI Chat:</strong> Engage in intelligent conversations powered by Google's Gemini AI
                    </div>
                    
                    <div class="feature">
                        <strong>💬 Multiple Conversations:</strong> Create, manage, and switch between different chat sessions
                    </div>
                    
                    <div class="feature">
                        <strong>🎨 Image Generation:</strong> Generate beautiful images using AI technology
                    </div>
                    
                    <div class="feature">
                        <strong>🔒 Secure & Private:</strong> Your conversations are securely stored and protected
                    </div>
                    
                    <p>Ready to get started? <a href="http://localhost:8080/chat.html">Start chatting now!</a></p>
                    
                    <p>If you have any questions or need assistance, don't hesitate to reach out to our support team.</p>
                    
                    <div class="footer">
                        <p>Thank you for choosing VFChatAI!</p>
                        <p>&copy; 2025 VFChatAI. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }
}
