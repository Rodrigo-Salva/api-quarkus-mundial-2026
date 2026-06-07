package com.mundial2026.predictions.kyc.dto;

import java.time.LocalDateTime;

public record KycAdminEntry(
        Long id,
        Long userId,
        String username,
        String status,
        String documentType,
        String documentNumber,
        LocalDateTime submittedAt,
        LocalDateTime verifiedAt
) {}
