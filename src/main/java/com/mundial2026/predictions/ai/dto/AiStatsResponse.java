package com.mundial2026.predictions.ai.dto;

public record AiStatsResponse(
        String provider,
        String analysis,
        String prediction,
        String tip
) {}
