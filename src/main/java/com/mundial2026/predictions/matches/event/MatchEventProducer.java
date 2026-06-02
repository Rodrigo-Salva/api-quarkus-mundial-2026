package com.mundial2026.predictions.matches.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.Map;

@ApplicationScoped
public class MatchEventProducer {

    @Inject
    @Channel("match-events-out")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    public void publishMatchUpdated(Long matchId, String status, Integer homeScore, Integer awayScore) {
        try {
            Map<String, Object> event = Map.of(
                    "type", "MATCH_UPDATED",
                    "matchId", matchId,
                    "status", status,
                    "homeScore", homeScore != null ? homeScore : 0,
                    "awayScore", awayScore != null ? awayScore : 0,
                    "timestamp", System.currentTimeMillis()
            );
            emitter.send(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish match event", e);
        }
    }

    public void publishMatchStarted(Long matchId) {
        try {
            Map<String, Object> event = Map.of(
                    "type", "MATCH_STARTED",
                    "matchId", matchId,
                    "timestamp", System.currentTimeMillis()
            );
            emitter.send(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish match started event", e);
        }
    }
}
