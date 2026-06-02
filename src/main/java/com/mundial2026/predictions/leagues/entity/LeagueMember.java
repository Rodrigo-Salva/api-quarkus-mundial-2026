package com.mundial2026.predictions.leagues.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "league_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"league_id", "user_id"}))
public class LeagueMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "league_id", nullable = false)
    public Long leagueId;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    public LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }
}
