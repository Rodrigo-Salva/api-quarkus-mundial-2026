package com.mundial2026.predictions.betting.dto;

import com.mundial2026.predictions.betting.entity.Bet;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BetResponse(
        Long id,
        Long matchId,
        String market,
        BigDecimal stake,
        BigDecimal odds,
        BigDecimal potentialPayout,
        String status,
        LocalDateTime createdAt
) {
    public static BetResponse from(Bet b) {
        return new BetResponse(b.id, b.matchId, b.market, b.stake,
                b.odds, b.potentialPayout, b.status, b.createdAt);
    }
}
