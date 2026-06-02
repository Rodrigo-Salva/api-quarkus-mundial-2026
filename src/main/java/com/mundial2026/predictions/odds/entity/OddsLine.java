package com.mundial2026.predictions.odds.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "odds_lines",
       uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "market"}))
public class OddsLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "match_id", nullable = false)
    public Long matchId;

    @Column(nullable = false, length = 30)
    public String market;

    @Column(nullable = false, precision = 8, scale = 2)
    public BigDecimal odds;

    // Cuota base sin ajuste dinámico — referencia para el cálculo
    @Column(name = "base_odds", precision = 8, scale = 2)
    public BigDecimal baseOdds;

    // Volumen total apostado en este mercado (activa el ajuste dinámico)
    @Column(name = "total_staked", nullable = false, precision = 14, scale = 2)
    public BigDecimal totalStaked = BigDecimal.ZERO;

    @Column(name = "bet_count", nullable = false)
    public Integer betCount = 0;

    @Column(nullable = false)
    public Boolean active = true;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
