package com.example.collabboard.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtUtil {

    private static final String DEFAULT_SECRET_KEY = "ChangeThisDefaultSecretKeyForCollabBoardInProduction123456";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours
    private String secretKey;

    @PostConstruct
    public void init() {
        String configured = System.getenv("COLLABBOARD_JWT_SECRET");
        this.secretKey = (configured == null || configured.trim().isEmpty())
            ? DEFAULT_SECRET_KEY
            : configured.trim();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token, String expectedUsername) {
        if (!validateToken(token)) {
            return false;
        }
        return Objects.equals(extractUsername(token), expectedUsername);
    }
}