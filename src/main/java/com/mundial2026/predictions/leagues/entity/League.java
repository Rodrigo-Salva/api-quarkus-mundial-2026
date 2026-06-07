package com.mundial2026.predictions.leagues.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leagues")
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, unique = true, length = 6)
    public String code;

    @Column(name = "owner_id", nullable = false)
    public Long ownerId;

    @Column(name = "image_url", length = 500)
    public String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false, length = 10)
    public String status = "ACTIVE";

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }
}
