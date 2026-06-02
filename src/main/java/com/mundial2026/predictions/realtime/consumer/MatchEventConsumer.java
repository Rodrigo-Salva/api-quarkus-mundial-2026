package com.mundial2026.predictions.realtime.consumer;

import com.mundial2026.predictions.matches.entity.MatchEvent;
import com.mundial2026.predictions.matches.repository.MatchEventRepository;
import com.mundial2026.predictions.realtime.service.LiveMatchService;
import com.mundial2026.predictions.realtime.websocket.MatchSocket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class MatchEventConsumer {

    @Inject
    LiveMatchService liveMatchService;

    @Inject
    MatchEventRepository matchEventRepository;

    @Incoming("match-events-in")
    public void consume(String message) {
        String matchId = extractMatchId(message);
        if (matchId != null) {
            // Redis: estado en vivo
            liveMatchService.updateMatchState(Long.parseLong(matchId), message);
            // WebSocket: broadcast a clientes conectados
            MatchSocket.broadcastToMatch(matchId, message);
            // DynamoDB: historial de eventos del partido
            MatchEvent event = MatchEvent.of(matchId, "MATCH_EVENT", null, null, null);
            matchEventRepository.save(event);
        }
    }

    private String extractMatchId(String json) {
        String key = "\"matchId\":";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return null;
        return json.substring(start, end).trim().replace("\"", "");
    }
}
