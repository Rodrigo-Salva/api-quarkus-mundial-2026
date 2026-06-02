package com.mundial2026.predictions.kyc.repository;

import com.mundial2026.predictions.kyc.entity.SelfExclusion;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ExclusionRepository implements PanacheRepository<SelfExclusion> {

    public List<SelfExclusion> findActiveByUserId(Long userId) {
        return list("userId = ?1 and active = true", userId);
    }
}
