package com.mundial2026.predictions.kyc.dto;

import java.time.LocalDateTime;

public record SelfExclusionRequest(
        LocalDateTime endDate,
        String reason
) {}
