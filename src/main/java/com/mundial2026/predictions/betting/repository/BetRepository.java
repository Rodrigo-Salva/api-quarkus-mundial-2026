package com.mundial2026.predictions.betting.repository;

import com.mundial2026.predictions.betting.entity.Bet;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class BetRepository implements PanacheRepository<Bet> {

    public List<Bet> findByUserId(Long userId) {
        return list("userId = ?1 order by createdAt desc", userId);
    }

    public List<Bet> findPendingByMatchId(Long matchId) {
        return list("matchId = ?1 and status = 'PENDING'", matchId);
    }
}
