package com.mundial2026.predictions.kyc.dto;

import java.time.LocalDateTime;

public record KycStatusResponse(
        String status,
        String documentType,
        LocalDateTime verifiedAt,
        LocalDateTime createdAt
) {}
