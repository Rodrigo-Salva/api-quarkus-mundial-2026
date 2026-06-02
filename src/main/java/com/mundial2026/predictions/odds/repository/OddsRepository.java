package com.mundial2026.predictions.odds.repository;

import com.mundial2026.predictions.odds.entity.OddsLine;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OddsRepository implements PanacheRepository<OddsLine> {

    public List<OddsLine> findByMatchId(Long matchId) {
        return list("matchId = ?1 and active = true", matchId);
    }

    public Optional<OddsLine> findByMatchIdAndMarket(Long matchId, String market) {
        return find("matchId = ?1 and market = ?2 and active = true", matchId, market)
                .firstResultOptional();
    }
}
