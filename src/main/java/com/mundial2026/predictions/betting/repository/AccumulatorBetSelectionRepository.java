package com.mundial2026.predictions.betting.repository;

import com.mundial2026.predictions.betting.entity.AccumulatorBetSelection;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AccumulatorBetSelectionRepository implements PanacheRepository<AccumulatorBetSelection> {

    public List<AccumulatorBetSelection> findByAccumulatorBetId(Long accumulatorBetId) {
        return list("accumulatorBetId", accumulatorBetId);
    }
}
