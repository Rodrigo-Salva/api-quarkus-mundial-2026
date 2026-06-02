package com.mundial2026.predictions.rankings.dto;

public record RankingResponse(
        long position,
        String username,
        double points,
        String country
) {}
