package com.mundial2026.predictions.betting.dto;

import com.mundial2026.predictions.betting.entity.AccumulatorBet;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccumulatorBetResponse(
        Long id,
        BigDecimal totalStake,
        BigDecimal totalOdds,
        BigDecimal potentialPayout,
        String status,
        LocalDateTime createdAt
) {
    public static AccumulatorBetResponse from(AccumulatorBet b) {
        return new AccumulatorBetResponse(b.id, b.totalStake, b.totalOdds,
                b.potentialPayout, b.status, b.createdAt);
    }
}
