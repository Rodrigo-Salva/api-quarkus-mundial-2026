package com.mundial2026.predictions.odds.repository;

import com.mundial2026.predictions.odds.entity.PlayerMarket;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PlayerMarketRepository implements PanacheRepository<PlayerMarket> {

    public List<PlayerMarket> findByMatchId(Long matchId) {
        return list("matchId = ?1 and active = true", matchId);
    }

    public Optional<PlayerMarket> findByMatchIdPlayerAndMarket(Long matchId, String playerName, String market) {
        return find("matchId = ?1 and playerName = ?2 and market = ?3", matchId, playerName, market)
                .firstResultOptional();
    }
}
