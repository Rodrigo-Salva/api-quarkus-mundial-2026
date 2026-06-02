package com.mundial2026.predictions.matches.dto;

import com.mundial2026.predictions.matches.entity.Match;
import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        String homeTeam,
        String awayTeam,
        Integer homeScore,
        Integer awayScore,
        String status,
        LocalDateTime matchDate
) {
    public static MatchResponse from(Match m) {
        return new MatchResponse(
                m.id, m.homeTeam, m.awayTeam,
                m.homeScore, m.awayScore,
                m.status.name(), m.matchDate);
    }
}
