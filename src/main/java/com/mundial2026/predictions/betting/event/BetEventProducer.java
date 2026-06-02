package com.mundial2026.predictions.betting.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundial2026.predictions.betting.entity.Bet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.Map;

@ApplicationScoped
public class BetEventProducer {

    @Inject
    @Channel("bets-out")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    public void publishBetPlaced(Bet bet) {
        try {
            Map<String, Object> event = Map.of(
                    "type", "BET_PLACED",
                    "betId", bet.id,
                    "userId", bet.userId,
                    "matchId", bet.matchId,
                    "market", bet.market,
                    "stake", bet.stake,
                    "odds", bet.odds,
                    "timestamp", System.currentTimeMillis()
            );
            emitter.send(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish bet event", e);
        }
    }
}
