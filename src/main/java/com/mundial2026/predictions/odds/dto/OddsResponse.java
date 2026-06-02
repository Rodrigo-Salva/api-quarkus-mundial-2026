package com.mundial2026.predictions.odds.dto;

import com.mundial2026.predictions.odds.entity.OddsLine;
import java.math.BigDecimal;

public record OddsResponse(
        Long matchId,
        String market,
        BigDecimal odds,
        BigDecimal baseOdds,
        BigDecimal totalStaked,
        Integer betCount,
        Boolean active
) {
    public static OddsResponse from(OddsLine line) {
        return new OddsResponse(
                line.matchId, line.market, line.odds,
                line.baseOdds, line.totalStaked, line.betCount, line.active);
    }
}
