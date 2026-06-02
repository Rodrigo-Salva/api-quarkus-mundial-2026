package com.mundial2026.predictions.betting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accumulator_bets")
public class AccumulatorBet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "total_stake", nullable = false, precision = 12, scale = 2)
    public BigDecimal totalStake;

    @Column(name = "total_odds", nullable = false, precision = 10, scale = 2)
    public BigDecimal totalOdds;

    @Column(name = "potential_payout", nullable = false, precision = 14, scale = 2)
    public BigDecimal potentialPayout;

    @Column(nullable = false, length = 20)
    public String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
