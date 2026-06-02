package com.mundial2026.predictions.auth.service;

import com.mundial2026.predictions.auth.dto.AuthResponse;
import com.mundial2026.predictions.auth.dto.LoginRequest;
import com.mundial2026.predictions.auth.dto.RegisterRequest;
import com.mundial2026.predictions.auth.entity.User;
import com.mundial2026.predictions.auth.repository.UserRepository;
import com.mundial2026.predictions.shared.security.JwtUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        User user = new User();
        user.email = req.email();
        user.username = req.username();
        user.passwordHash = hashPassword(req.password());
        user.country = req.country().toUpperCase();
        user.role = "USER";

        userRepository.persist(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Correo electrónico o contraseña incorrectos"));

        if (!user.passwordHash.equals(hashPassword(req.password()))) {
            throw new IllegalArgumentException("Correo electrónico o contraseña incorrectos");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        try {
            String[] parts = refreshToken.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid refresh token");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            String subject = extractClaim(payload, "sub");
            Long userId = Long.parseLong(subject);

            User user = userRepository.findById(userId);
            if (user == null) throw new NotFoundException("User not found");

            return buildAuthResponse(user);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("El token de renovación es inválido o ha expirado. Por favor inicia sesión de nuevo");
        }
    }

    public User findById(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) throw new NotFoundException("User not found");
        return user;
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.id, user.email, user.role);
        String refreshToken = jwtUtil.generateRefreshToken(user.id, user.email);
        var userInfo = new AuthResponse.UserInfo(user.id, user.email, user.username, user.country, user.role);
        return new AuthResponse(accessToken, refreshToken, userInfo);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String extractClaim(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) throw new IllegalArgumentException("Claim not found: " + key);
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
