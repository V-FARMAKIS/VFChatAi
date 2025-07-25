package com.example.VF_ChatAi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Custom filter to handle session-based authentication for Spring Security
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private SessionAuthenticationComponent sessionAuth;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }


        Optional<String> userEmail = sessionAuth.getCurrentUserEmail();

        if (userEmail.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userEmail.get(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );


            SecurityContextHolder.getContext().setAuthentication(authToken);


            sessionAuth.updateLastAccess();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the endpoint should be publicly accessible
     */
    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/api/auth/") ||
                requestPath.startsWith("/api/maintenance/") ||
                requestPath.equals("/api/ai/health") ||
                requestPath.startsWith("/api/ai/test/") ||
                requestPath.startsWith("/css/") ||
                requestPath.startsWith("/js/") ||
                requestPath.startsWith("/images/") ||
                requestPath.startsWith("/static/") ||
                requestPath.equals("/") ||
                requestPath.equals("/index.html") ||
                requestPath.equals("/landingpage.html") ||
                requestPath.equals("/verify-email.html") ||
                requestPath.equals("/reset-password.html") ||
                requestPath.equals("/account-success.html") ||
                requestPath.equals("/favicon.ico");
    }
}