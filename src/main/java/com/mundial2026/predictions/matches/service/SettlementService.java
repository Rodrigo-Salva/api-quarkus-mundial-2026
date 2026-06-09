package com.mundial2026.predictions.matches.service;

import com.mundial2026.predictions.leagues.repository.LeagueMemberRepository;
import com.mundial2026.predictions.matches.entity.Match;
import com.mundial2026.predictions.matches.repository.MatchRepository;
import com.mundial2026.predictions.notifications.websocket.NotificationSocket;
import com.mundial2026.predictions.predictions.repository.PredictionRepository;
import com.mundial2026.predictions.predictions.service.PredictionService;
import com.mundial2026.predictions.rankings.service.RankingService;
import com.mundial2026.predictions.auth.repository.UserRepository;
import com.mundial2026.predictions.realtime.websocket.MatchSocket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * Orquesta la liquidación completa cuando el admin registra un resultado:
 *  1. Calcula los puntos de todas las predicciones del partido (5 reglas)
 *  2. Actualiza el ranking global en Redis
 *  3. Notifica por WebSocket a los usuarios conectados
 */
@ApplicationScoped
public class SettlementService {

    private static final Logger LOG = Logger.getLogger(SettlementService.class);

    @Inject PredictionService      predictionService;
    @Inject PredictionRepository   predictionRepository;
    @Inject RankingService         rankingService;
    @Inject UserRepository         userRepository;
    @Inject LeagueMemberRepository leagueMemberRepository;
    @Inject MatchRepository        matchRepository;

    /**
     * Punto de entrada principal — llamado desde MatchService cuando el admin
     * registra el resultado de un partido.
     */
    @Transactional
    public void settleMatch(Long matchId, Integer homeScore, Integer awayScore,
                            LocalDateTime matchDate) {
        LOG.infof("Liquidando partido %d: %d-%d", matchId, homeScore, awayScore);

        // 1. Calcular puntos de todas las predicciones (las 5 reglas)
        predictionService.calculatePoints(matchId, homeScore, awayScore, matchDate);

        // 2. Actualizar ranking global en Redis para cada usuario afectado
        updateRankingsForMatch(matchId);

        // 3. Notificar por WebSocket al partido y a todos los usuarios afectados
        notifySettlement(matchId, homeScore, awayScore);

        LOG.infof("Partido %d liquidado correctamente", matchId);
    }

    // Recorre todas las predicciones del partido y actualiza Redis
    private void updateRankingsForMatch(Long matchId) {
        predictionRepository.findByMatchId(matchId).forEach(prediction -> {
            try {
                userRepository.findByIdOptional(prediction.userId)
                        .map(user -> {
                            // Suma todos los totalPoints del usuario
                            double totalPoints = predictionRepository
                                    .findSettledByUserIdOrderedByDate(prediction.userId)
                                    .stream()
                                    .mapToInt(p -> p.totalPoints != null ? p.totalPoints : 0)
                                    .sum();
                            rankingService.addOrUpdateScore(
                                    user.id, user.username, user.country, totalPoints);
                            return user;
                        });
            } catch (Exception e) {
                LOG.warnf("No se pudo actualizar ranking para userId=%d: %s",
                        prediction.userId, e.getMessage());
            }
        });
    }

    // Envía notificaciones WebSocket
    private void notifySettlement(Long matchId, Integer homeScore, Integer awayScore) {
        try {
            Match match = matchRepository.findById(matchId);
            String homeTeam = match != null ? match.homeTeam : "Local";
            String awayTeam = match != null ? match.awayTeam : "Visitante";

            // Notifica al canal del partido
            String matchEvent = String.format(
                    "{\"type\":\"MATCH_FINISHED\",\"matchId\":%d,\"homeScore\":%d,\"awayScore\":%d"
                    + ",\"homeTeam\":\"%s\",\"awayTeam\":\"%s\"}",
                    matchId, homeScore, awayScore, homeTeam, awayTeam);
            MatchSocket.broadcastToMatch(String.valueOf(matchId), matchEvent);

            // Notifica a cada usuario con su resultado personalizado + desglose completo
            predictionRepository.findByMatchId(matchId).forEach(prediction -> {
                try {
                    boolean exactScore = prediction.homeScore.equals(homeScore)
                            && prediction.awayScore.equals(awayScore);
                    int predDiff = prediction.homeScore - prediction.awayScore;
                    int realDiff = homeScore - awayScore;
                    boolean diffCorrect = !exactScore && predDiff == realDiff;

                    String userNotif = String.format(
                            "{\"type\":\"PREDICTION_SETTLED\",\"matchId\":%d"
                            + ",\"homeTeam\":\"%s\",\"awayTeam\":\"%s\""
                            + ",\"homeScore\":%d,\"awayScore\":%d"
                            + ",\"predictedHome\":%d,\"predictedAway\":%d"
                            + ",\"points\":%d,\"earlyBonus\":%d,\"streakBonus\":%d"
                            + ",\"totalPoints\":%d,\"winnerCorrect\":%b"
                            + ",\"exactScore\":%b,\"diffCorrect\":%b}",
                            matchId,
                            homeTeam, awayTeam,
                            homeScore, awayScore,
                            prediction.homeScore, prediction.awayScore,
                            prediction.points,
                            prediction.earlyBonus,
                            prediction.streakBonus,
                            prediction.totalPoints,
                            prediction.winnerCorrect,
                            exactScore,
                            diffCorrect);
                    NotificationSocket.sendToUser(
                            String.valueOf(prediction.userId), userNotif);
                } catch (Exception e) {
                    LOG.warnf("No se pudo notificar a userId=%d", prediction.userId);
                }
            });
        } catch (Exception e) {
            LOG.warn("Error enviando notificaciones WebSocket: " + e.getMessage());
        }
    }
}
