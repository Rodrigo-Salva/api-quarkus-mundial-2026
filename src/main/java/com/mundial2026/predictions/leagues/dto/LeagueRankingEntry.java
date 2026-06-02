package com.mundial2026.predictions.leagues.dto;

import java.time.LocalDateTime;

public record LeagueRankingEntry(
        long          position,
        Long          userId,
        String        username,
        String        country,
        int           points,
        LocalDateTime joinedAt     // fecha desde cuando se cuentan los puntos
) {}
