package com.mundial2026.predictions.kyc.repository;

import com.mundial2026.predictions.kyc.entity.KycVerification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class KycRepository implements PanacheRepository<KycVerification> {

    public Optional<KycVerification> findByUserId(Long userId) {
        return find("userId", userId).firstResultOptional();
    }

    public List<KycVerification> findAllOrderedByCreated() {
        return list("ORDER BY createdAt DESC");
    }

    public List<KycVerification> findByStatus(String status) {
        return list("status = ?1 ORDER BY createdAt DESC", status);
    }
}
