package com.mundial2026.predictions.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserInfo user
) {
    public record UserInfo(Long id, String email, String username, String country, String role) {}
}
