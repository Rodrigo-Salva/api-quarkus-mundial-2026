package com.mundial2026.predictions.odds.dto;

import com.mundial2026.predictions.odds.entity.PlayerMarket;
import java.math.BigDecimal;

public record PlayerMarketResponse(
        Long matchId,
        String playerName,
        String market,
        BigDecimal odds,
        BigDecimal baseOdds,
        BigDecimal totalStaked,
        Integer betCount
) {
    public static PlayerMarketResponse from(PlayerMarket p) {
        return new PlayerMarketResponse(
                p.matchId, p.playerName, p.market,
                p.odds, p.baseOdds, p.totalStaked, p.betCount);
    }
}
