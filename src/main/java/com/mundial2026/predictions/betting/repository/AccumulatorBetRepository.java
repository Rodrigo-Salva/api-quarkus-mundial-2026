package com.mundial2026.predictions.betting.repository;

import com.mundial2026.predictions.betting.entity.AccumulatorBet;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AccumulatorBetRepository implements PanacheRepository<AccumulatorBet> {

    public List<AccumulatorBet> findByUserId(Long userId) {
        return list("userId = ?1 order by createdAt desc", userId);
    }
}
