package com.mundial2026.predictions.predictions.service;

import com.mundial2026.predictions.leagues.repository.LeagueMemberRepository;
import com.mundial2026.predictions.matches.dto.MatchResponse;
import com.mundial2026.predictions.matches.service.MatchService;
import com.mundial2026.predictions.predictions.dto.PredictionDetailResponse;
import com.mundial2026.predictions.predictions.dto.PredictionRequest;
import com.mundial2026.predictions.predictions.dto.PredictionResponse;
import com.mundial2026.predictions.predictions.entity.Prediction;
import com.mundial2026.predictions.predictions.repository.PredictionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reglas de puntuación del laboratorio:
 *  1) Resultado exacto      → 5 pts
 *  2) Ganador correcto      → 3 pts
 *  3) Diferencia correcta   → 2 pts (mismo margen, mismo ganador, marcador distinto)
 *  4) Bonus racha           → +2 pts en la predicción que completa 3 consecutivos acertados
 *  5) Predicción anticipada → +1 pt si se registró >24h antes del partido
 */
@ApplicationScoped
public class PredictionService {

    @Inject
    PredictionRepository predictionRepository;

    @Inject
    MatchService matchService;

    @Inject
    LeagueMemberRepository leagueMemberRepository;

    @Transactional
    public PredictionResponse submit(Long userId, PredictionRequest req) {
        // Debe pertenecer a al menos una sala para poder predecir
        if (!leagueMemberRepository.isInAnyLeague(userId)) {
            throw new IllegalStateException(
                    "Debes unirte o crear una sala antes de poder hacer predicciones");
        }

        MatchResponse match = matchService.findById(req.matchId());
        if (!"SCHEDULED".equals(match.status())) {
            throw new IllegalArgumentException("Solo puedes predecir partidos que aún no han comenzado");
        }

        predictionRepository.findByUserIdAndMatchId(userId, req.matchId())
                .ifPresent(p -> { throw new IllegalArgumentException("Ya enviaste una predicción para este partido"); });

        Prediction prediction = new Prediction();
        prediction.userId    = userId;
        prediction.matchId   = req.matchId();
        prediction.homeScore = req.homeScore();
        prediction.awayScore = req.awayScore();
        prediction.points    = 0;

        predictionRepository.persist(prediction);
        return PredictionResponse.from(prediction);
    }

    public List<PredictionResponse> findByUser(Long userId) {
        return predictionRepository.findByUserId(userId).stream()
                .map(PredictionResponse::from)
                .collect(Collectors.toList());
    }

    public List<PredictionResponse> findByMatch(Long matchId) {
        return predictionRepository.findByMatchId(matchId).stream()
                .map(PredictionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Desglose completo de puntos de una predicción.
     * El usuario ve exactamente por qué ganó o perdió cada punto.
     */
    public PredictionDetailResponse getDetail(Long predictionId, Long userId) {
        Prediction p = predictionRepository.findById(predictionId);
        if (p == null) throw new jakarta.ws.rs.NotFoundException("Predicción no encontrada");
        if (!p.userId.equals(userId)) throw new SecurityException("No tienes acceso a esta predicción");

        MatchResponse match = matchService.findById(p.matchId);
        return PredictionDetailResponse.from(
                p,
                match.homeTeam(), match.awayTeam(),
                match.homeScore(), match.awayScore(),
                match.status(), match.matchDate());
    }

    /**
     * Historial detallado completo del usuario — todas sus predicciones con desglose.
     */
    public List<PredictionDetailResponse> getDetailedHistory(Long userId) {
        return predictionRepository.findByUserId(userId).stream()
                .map(p -> {
                    MatchResponse match = matchService.findById(p.matchId);
                    return PredictionDetailResponse.from(
                            p,
                            match.homeTeam(), match.awayTeam(),
                            match.homeScore(), match.awayScore(),
                            match.status(), match.matchDate());
                })
                .collect(Collectors.toList());
    }

    /**
     * Liquida todas las predicciones de un partido aplicando las 5 reglas.
     */
    @Transactional
    public void calculatePoints(Long matchId, Integer actualHome, Integer actualAway,
                                LocalDateTime matchDate) {
        List<Prediction> predictions = predictionRepository.findByMatchId(matchId);

        for (Prediction p : predictions) {
            p.points        = computeBasePoints(p.homeScore, p.awayScore, actualHome, actualAway);
            p.winnerCorrect = isWinnerCorrect(p.homeScore, p.awayScore, actualHome, actualAway);
            p.earlyBonus    = computeEarlyBonus(p.createdAt, matchDate);
            p.streakBonus   = 0;
            p.settled       = true;
            p.totalPoints   = p.points + p.earlyBonus;
        }

        // Recalcular racha para cada usuario del partido
        predictions.stream()
                .map(p -> p.userId)
                .distinct()
                .forEach(this::recalculateStreakBonus);
    }

    /** Versión simplificada — obtiene matchDate del propio partido. */
    @Transactional
    public void calculatePoints(Long matchId, Integer actualHome, Integer actualAway) {
        MatchResponse match = matchService.findById(matchId);
        calculatePoints(matchId, actualHome, actualAway, match.matchDate());
    }

    /**
     * Regla 4: recorre todas las predicciones liquidadas del usuario en orden cronológico.
     * Cada vez que la racha llega a un múltiplo de 3, la predicción que lo completa recibe +2.
     */
    @Transactional
    public void recalculateStreakBonus(Long userId) {
        List<Prediction> settled = predictionRepository.findSettledByUserIdOrderedByDate(userId);
        int streak = 0;
        for (Prediction p : settled) {
            if (Boolean.TRUE.equals(p.winnerCorrect)) {
                streak++;
            } else {
                streak = 0;
            }
            p.streakBonus = (streak > 0 && streak % 3 == 0) ? 2 : 0;
            p.totalPoints = p.points + p.earlyBonus + p.streakBonus;
        }
    }

    // ── Cálculos de puntos ───────────────────────────────────────

    /**
     * Regla 1: marcador exacto → 5 pts
     * Regla 3: mismo ganador + misma diferencia de goles → 2 pts
     * Regla 2: ganador correcto (diferencia distinta) → 3 pts
     */
    int computeBasePoints(int predHome, int predAway, int actualHome, int actualAway) {
        if (predHome == actualHome && predAway == actualAway) return 5;

        int predSign   = Integer.compare(predHome, predAway);
        int actualSign = Integer.compare(actualHome, actualAway);

        if (predSign == actualSign) {
            int predDiff   = Math.abs(predHome - predAway);
            int actualDiff = Math.abs(actualHome - actualAway);
            if (predDiff == actualDiff) return 2;  // Regla 3
            return 3;                               // Regla 2
        }
        return 0;
    }

    boolean isWinnerCorrect(int predHome, int predAway, int actualHome, int actualAway) {
        return Integer.compare(predHome, predAway) == Integer.compare(actualHome, actualAway);
    }

    /** Regla 5: +1 si la predicción se registró más de 24h antes del partido. */
    int computeEarlyBonus(LocalDateTime predictionTime, LocalDateTime matchDate) {
        if (predictionTime == null || matchDate == null) return 0;
        return ChronoUnit.HOURS.between(predictionTime, matchDate) > 24 ? 1 : 0;
    }
}
