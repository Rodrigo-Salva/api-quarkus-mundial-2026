package com.mundial2026.predictions.matches.entity;

import java.util.Map;

/**
 * Evento de partido almacenado en DynamoDB.
 * PK: matchId  SK: timestamp#eventId
 */
public class MatchEvent {

    public String matchId;
    public String timestamp;   // ISO-8601 + "#" + UUID para unicidad como sort key
    public String type;        // MATCH_STARTED, MATCH_UPDATED, GOAL, CARD, etc.
    public String status;
    public Integer homeScore;
    public Integer awayScore;
    public Map<String, String> extra; // datos adicionales según tipo de evento

    public static MatchEvent of(String matchId, String type, String status,
                                Integer homeScore, Integer awayScore) {
        MatchEvent e = new MatchEvent();
        e.matchId = matchId;
        e.timestamp = java.time.Instant.now() + "#" + java.util.UUID.randomUUID();
        e.type = type;
        e.status = status;
        e.homeScore = homeScore;
        e.awayScore = awayScore;
        return e;
    }
}
