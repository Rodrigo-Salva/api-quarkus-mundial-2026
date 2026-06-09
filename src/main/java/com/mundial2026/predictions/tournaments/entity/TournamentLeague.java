package com.mundial2026.predictions.tournaments.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_leagues",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "league_id"}))
public class TournamentLeague {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "tournament_id", nullable = false)
    public Long tournamentId;

    @Column(name = "league_id", nullable = false)
    public Long leagueId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    public LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }
}
