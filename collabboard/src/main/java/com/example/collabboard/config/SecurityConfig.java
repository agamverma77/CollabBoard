package com.example.collabboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF: Not needed for a non-browser client and can interfere with WebSocket handshakes.
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Define Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // 3. THIS IS THE FIX: Explicitly permit all HTTP requests to the WebSocket endpoint.
                .requestMatchers("/ws/**").permitAll()
                
                // 4. Secure all other possible endpoints by default.
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
}