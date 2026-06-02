package com.mundial2026.predictions.shared.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class SecurityUtils {

    @Inject
    JsonWebToken jwt;

    public Long getCurrentUserId() {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new SecurityException("No authenticated user");
        }
        return Long.parseLong(subject);
    }

    public String getCurrentUserEmail() {
        return jwt.getClaim("email");
    }

    public String getCurrentUserRole() {
        return jwt.getClaim("role");
    }

    public boolean hasRole(String role) {
        return jwt.getGroups().contains(role);
    }
}
