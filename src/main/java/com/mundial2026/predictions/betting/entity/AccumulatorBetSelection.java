package com.mundial2026.predictions.betting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accumulator_bet_selections")
public class AccumulatorBetSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "accumulator_bet_id", nullable = false)
    public Long accumulatorBetId;

    @Column(name = "match_id", nullable = false)
    public Long matchId;

    @Column(nullable = false, length = 30)
    public String market;

    @Column(nullable = false, precision = 8, scale = 2)
    public BigDecimal odds;

    @Column(nullable = false, length = 20)
    public String result = "PENDING";
}
