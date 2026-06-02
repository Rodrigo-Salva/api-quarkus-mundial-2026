package com.mundial2026.predictions.kyc.repository;

import com.mundial2026.predictions.kyc.entity.GamblingLimit;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LimitRepository implements PanacheRepository<GamblingLimit> {

    public List<GamblingLimit> findByUserId(Long userId) {
        return list("userId", userId);
    }

    public Optional<GamblingLimit> findByUserIdAndType(Long userId, String limitType) {
        return find("userId = ?1 and limitType = ?2", userId, limitType).firstResultOptional();
    }
}
