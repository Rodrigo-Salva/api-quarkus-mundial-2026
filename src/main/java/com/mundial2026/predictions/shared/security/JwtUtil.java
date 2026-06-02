package com.mundial2026.predictions.shared.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtUtil {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String generateAccessToken(Long userId, String email, String role) {
        return Jwt.issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .groups(Set.of(role))
                .expiresIn(Duration.ofMinutes(15))
                .sign();
    }

    public String generateRefreshToken(Long userId, String email) {
        return Jwt.issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("type", "refresh")
                .expiresIn(Duration.ofDays(7))
                .sign();
    }
}
