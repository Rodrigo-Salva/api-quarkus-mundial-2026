package com.mundial2026.predictions.auth.service;

import com.mundial2026.predictions.auth.dto.AuthResponse;
import com.mundial2026.predictions.auth.dto.LoginRequest;
import com.mundial2026.predictions.auth.dto.RegisterRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AuthServiceTest {

    @Inject
    AuthService authService;

    private String email(String prefix) {
        return prefix + System.nanoTime() + "@test.com";
    }

    private String username(String prefix) {
        return prefix + System.nanoTime();
    }

    @Test
    void testRegisterAndLogin() {
        String e = email("register");
        RegisterRequest req = new RegisterRequest(e, username("user"), "password123", "ARG");
        AuthResponse response = authService.register(req);

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("ARG", response.user().country());
    }

    @Test
    void testLoginWithWrongPassword() {
        String e = email("wrong");
        authService.register(new RegisterRequest(e, username("wrong"), "correctpass", "BRA"));

        LoginRequest login = new LoginRequest(e, "wrongpass");
        assertThrows(IllegalArgumentException.class, () -> authService.login(login));
    }

    @Test
    void testDuplicateEmail() {
        String e = email("dup");
        authService.register(new RegisterRequest(e, username("dup1"), "pass1234", "ESP"));

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest(e, username("dup2"), "pass5678", "ESP")));
    }
}
