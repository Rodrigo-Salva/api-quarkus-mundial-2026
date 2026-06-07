package com.mundial2026.predictions.leagues.repository;

import com.mundial2026.predictions.leagues.entity.LeagueMember;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LeagueMemberRepository implements PanacheRepository<LeagueMember> {

    public List<LeagueMember> findByLeagueId(Long leagueId) {
        return list("leagueId", leagueId);
    }

    public List<LeagueMember> findByUserId(Long userId) {
        return list("userId", userId);
    }

    public boolean isMember(Long leagueId, Long userId) {
        return count("leagueId = ?1 and userId = ?2", leagueId, userId) > 0;
    }

    // Verifica si el usuario pertenece a AL MENOS una sala
    public boolean isInAnyLeague(Long userId) {
        return count("userId", userId) > 0;
    }

    // Fecha en que el usuario se unió a una sala específica
    public Optional<LocalDateTime> findJoinedAt(Long leagueId, Long userId) {
        return find("leagueId = ?1 and userId = ?2", leagueId, userId)
                .firstResultOptional()
                .map(m -> m.joinedAt);
    }

    // Todos los miembros de una sala con su fecha de ingreso
    public List<LeagueMember> findByLeagueIdOrdered(Long leagueId) {
        return list("leagueId = ?1 order by joinedAt asc", leagueId);
    }

    public boolean removeMember(Long leagueId, Long userId) {
        return delete("leagueId = ?1 and userId = ?2", leagueId, userId) > 0;
    }
}
