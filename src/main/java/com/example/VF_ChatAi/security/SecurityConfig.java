package com.example.VF_ChatAi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SessionAuthenticationFilter sessionAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))


                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(authz -> authz

                        .requestMatchers("/", "/index.html", "/landingpage.html", "/verify-email.html",
                                "/reset-password.html", "/account-success.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/generated-images/**",
                                "/favicon.ico", "/static/**").permitAll()


                        .requestMatchers("/api/auth/**").permitAll()


                        .requestMatchers("/api/maintenance/**").permitAll()
                        .requestMatchers("/api/ai/health").permitAll()
                        .requestMatchers("/api/ai/test/**").permitAll()


                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/ai/chat").authenticated()
                        .requestMatchers("/api/ai/generate-image").authenticated()


                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(5) // Allow multiple sessions per user
                        .maxSessionsPreventsLogin(false)
                        .and()
                        .invalidSessionUrl("/landingpage.html")
                        .sessionFixation().migrateSession()
                )
                .formLogin(form -> form.disable()) // Disable default form login
                .httpBasic(basic -> basic.disable()) // Disable basic auth
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/landingpage.html")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "VFCHATAI_SESSION")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {

                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"success\":false,\"error\":\"Authentication required\",\"errorCode\":\"UNAUTHORIZED\"}");
                            } else {

                                response.sendRedirect("/landingpage.html");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"success\":false,\"error\":\"Access denied\",\"errorCode\":\"FORBIDDEN\"}");
                            } else {
                                response.sendRedirect("/landingpage.html");
                            }
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}