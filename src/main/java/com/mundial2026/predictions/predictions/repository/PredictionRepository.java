package com.mundial2026.predictions.predictions.repository;

import com.mundial2026.predictions.predictions.entity.Prediction;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PredictionRepository implements PanacheRepository<Prediction> {

    public List<Prediction> findByUserId(Long userId) {
        return list("userId", userId);
    }

    public List<Prediction> findByMatchId(Long matchId) {
        return list("matchId", matchId);
    }

    public Optional<Prediction> findByUserIdAndMatchId(Long userId, Long matchId) {
        return find("userId = ?1 and matchId = ?2", userId, matchId).firstResultOptional();
    }

    public List<Prediction> findSettledByUserIdOrderedByDate(Long userId) {
        return list("userId = ?1 and settled = true order by createdAt asc", userId);
    }

    // Suma totalPoints de predicciones liquidadas DESDE una fecha dada
    // (para ranking de sala: solo cuenta desde que el usuario se unió)
    public int sumPointsByUserSince(Long userId, LocalDateTime since) {
        return find("userId = ?1 and settled = true and createdAt >= ?2", userId, since)
                .stream()
                .mapToInt(p -> p.totalPoints != null ? p.totalPoints : 0)
                .sum();
    }
}
