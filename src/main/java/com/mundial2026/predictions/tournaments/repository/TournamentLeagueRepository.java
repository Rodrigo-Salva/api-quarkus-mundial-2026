package com.mundial2026.predictions.tournaments.repository;

import com.mundial2026.predictions.tournaments.entity.TournamentLeague;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TournamentLeagueRepository implements PanacheRepository<TournamentLeague> {

    public List<TournamentLeague> findByTournamentId(Long tournamentId) {
        return list("tournamentId", tournamentId);
    }

    public boolean isLeagueInTournament(Long tournamentId, Long leagueId) {
        return count("tournamentId = ?1 and leagueId = ?2", tournamentId, leagueId) > 0;
    }
}
