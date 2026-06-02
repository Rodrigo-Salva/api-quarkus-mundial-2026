package com.mundial2026.predictions.wallet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    public Long userId;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    public String currency = "PEN";

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
