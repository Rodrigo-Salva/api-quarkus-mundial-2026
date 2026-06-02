package com.mundial2026.predictions.shared;

import com.mundial2026.predictions.auth.dto.RegisterRequest;
import com.mundial2026.predictions.auth.entity.User;
import com.mundial2026.predictions.auth.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@ApplicationScoped
public class TestUserHelper {

    @Inject
    UserRepository userRepository;

    @Transactional
    public Long createUser(long seedId) {
        String email = "testuser" + seedId + "@test.com";
        return userRepository.findByEmail(email)
                .map(u -> u.id)
                .orElseGet(() -> {
                    User u = new User();
                    u.email = email;
                    u.username = "testuser" + seedId;
                    u.passwordHash = hash("pass1234");
                    u.country = "PER";
                    u.role = "USER";
                    userRepository.persist(u);
                    return u.id;
                });
    }

    private String hash(String pwd) {
        try {
            var d = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(d.digest(pwd.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
