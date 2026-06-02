package com.mundial2026.predictions.predictions.dto;

import com.mundial2026.predictions.predictions.entity.Prediction;
import java.time.LocalDateTime;

public record PredictionResponse(
        Long id,
        Long userId,
        Long matchId,
        Integer homeScore,
        Integer awayScore,
        Integer points,
        Integer earlyBonus,
        Integer streakBonus,
        Integer totalPoints,
        Boolean winnerCorrect,
        Boolean settled,
        LocalDateTime createdAt
) {
    public static PredictionResponse from(Prediction p) {
        return new PredictionResponse(
                p.id, p.userId, p.matchId,
                p.homeScore, p.awayScore,
                p.points, p.earlyBonus, p.streakBonus, p.totalPoints,
                p.winnerCorrect, p.settled, p.createdAt);
    }
}
