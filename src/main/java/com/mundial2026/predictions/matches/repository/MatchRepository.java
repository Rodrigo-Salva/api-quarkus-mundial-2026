package com.mundial2026.predictions.matches.repository;

import com.mundial2026.predictions.matches.entity.Match;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class MatchRepository implements PanacheRepository<Match> {

    public List<Match> findByStatus(Match.MatchStatus status) {
        return list("status", status);
    }

    public List<Match> findLive() {
        return list("status", Match.MatchStatus.LIVE);
    }
}
