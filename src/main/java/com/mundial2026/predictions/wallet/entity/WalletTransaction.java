package com.mundial2026.predictions.wallet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "wallet_id", nullable = false)
    public Long walletId;

    @Column(nullable = false, length = 20)
    public String type;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 14, scale = 2)
    public BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
    public BigDecimal balanceAfter;

    @Column(length = 100)
    public String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
