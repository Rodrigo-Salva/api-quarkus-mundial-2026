package com.mundial2026.predictions.predictions.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "match_id"}))
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "match_id", nullable = false)
    public Long matchId;

    @Column(name = "home_score", nullable = false)
    public Integer homeScore;

    @Column(name = "away_score", nullable = false)
    public Integer awayScore;

    // Puntos base de la predicción (sin bonus)
    @Column(nullable = false)
    public Integer points = 0;

    // +1 si la predicción se registró >24h antes del partido
    @Column(name = "early_bonus", nullable = false)
    public Integer earlyBonus = 0;

    // +2 por cada 3 partidos consecutivos acertados (calculado al liquidar)
    @Column(name = "streak_bonus", nullable = false)
    public Integer streakBonus = 0;

    // Suma de points + earlyBonus + streakBonus
    @Column(name = "total_points", nullable = false)
    public Integer totalPoints = 0;

    // true si acertó ganador o empate (usado para calcular rachas)
    @Column(name = "winner_correct", nullable = false)
    public Boolean winnerCorrect = false;

    // true cuando el partido terminó y los puntos fueron calculados
    @Column(nullable = false)
    public Boolean settled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
