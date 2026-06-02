package com.mundial2026.predictions.kyc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_verifications")
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    public Long userId;

    @Column(nullable = false)
    public String status = "PENDING";

    @Column(name = "document_type", nullable = false)
    public String documentType;

    @Column(name = "document_number", nullable = false)
    public String documentNumber;

    @Column(name = "verified_at")
    public LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
