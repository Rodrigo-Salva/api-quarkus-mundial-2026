package com.mundial2026.predictions.matches.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "home_team", nullable = false)
    public String homeTeam;

    @Column(name = "away_team", nullable = false)
    public String awayTeam;

    @Column(name = "home_score")
    public Integer homeScore;

    @Column(name = "away_score")
    public Integer awayScore;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "match_date", nullable = false)
    public LocalDateTime matchDate;

    public enum MatchStatus {
        SCHEDULED, LIVE, FINISHED
    }
}
