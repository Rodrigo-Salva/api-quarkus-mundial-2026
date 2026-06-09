package com.mundial2026.predictions.tournaments.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, length = 8, unique = true)
    public String code;

    @Column(name = "owner_id", nullable = false)
    public Long ownerId;

    @Column(nullable = false, length = 20)
    public String status = "OPEN";

    @Column(name = "private", nullable = false)
    public boolean isPrivate = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
