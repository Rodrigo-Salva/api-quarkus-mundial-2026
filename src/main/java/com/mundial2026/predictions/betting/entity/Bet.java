package com.mundial2026.predictions.betting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bets")
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "match_id", nullable = false)
    public Long matchId;

    @Column(nullable = false, length = 30)
    public String market;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal stake;

    @Column(nullable = false, precision = 8, scale = 2)
    public BigDecimal odds;

    @Column(name = "potential_payout", nullable = false, precision = 14, scale = 2)
    public BigDecimal potentialPayout;

    @Column(nullable = false, length = 20)
    public String status = "PENDING";

    @Column(name = "settled_at")
    public LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
