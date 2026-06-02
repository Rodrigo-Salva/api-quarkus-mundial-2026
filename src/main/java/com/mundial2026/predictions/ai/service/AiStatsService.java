package com.mundial2026.predictions.ai.service;

import com.mundial2026.predictions.ai.dto.AiStatsResponse;
import com.mundial2026.predictions.ai.provider.AiProvider;
import com.mundial2026.predictions.ai.provider.AiProviderSelector;
import com.mundial2026.predictions.predictions.dto.PredictionResponse;
import com.mundial2026.predictions.predictions.service.PredictionService;
import com.mundial2026.predictions.rankings.dto.RankingResponse;
import com.mundial2026.predictions.rankings.service.RankingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiStatsService {

    @Inject AiProviderSelector providerSelector;
    @Inject PredictionService  predictionService;
    @Inject RankingService     rankingService;

    /**
     * Analiza el historial de predicciones de un usuario y genera:
     * - Análisis de su rendimiento
     * - Predicción de su tendencia futura
     * - Tip personalizado para mejorar
     */
    public AiStatsResponse analyzeUser(Long userId) {
        AiProvider ai = providerSelector.get();

        List<PredictionResponse> predictions = predictionService.findByUser(userId);
        RankingResponse ranking = rankingService.getUserRanking(userId, "user" + userId, "PER");

        String prompt = buildUserAnalysisPrompt(userId, predictions, ranking);
        String response = ai.complete(prompt);

        // Parsear las 3 secciones de la respuesta
        return new AiStatsResponse(
                ai.providerName(),
                extractSection(response, "ANÁLISIS"),
                extractSection(response, "PREDICCIÓN"),
                extractSection(response, "TIP")
        );
    }

    /**
     * Analiza las tendencias globales del torneo.
     */
    public AiStatsResponse analyzeTournament() {
        AiProvider ai = providerSelector.get();

        List<RankingResponse> top10 = rankingService.getGlobalRanking(10);
        String prompt = buildTournamentPrompt(top10);
        String response = ai.complete(prompt);

        return new AiStatsResponse(
                ai.providerName(),
                extractSection(response, "ANÁLISIS"),
                extractSection(response, "PREDICCIÓN"),
                extractSection(response, "TIP")
        );
    }

    /**
     * Sugiere la predicción para un partido basándose en estadísticas históricas.
     */
    public AiStatsResponse suggestPrediction(String homeTeam, String awayTeam) {
        AiProvider ai = providerSelector.get();

        String prompt = String.format("""
                Eres un experto en análisis de fútbol del Mundial 2026.
                Analiza el partido: %s vs %s

                Responde EXACTAMENTE en este formato:
                ANÁLISIS: [análisis breve del partido en 2 oraciones]
                PREDICCIÓN: [marcador más probable, ej: 2-1]
                TIP: [consejo específico sobre en qué mercado apostar]
                """, homeTeam, awayTeam);

        String response = ai.complete(prompt);

        return new AiStatsResponse(
                ai.providerName(),
                extractSection(response, "ANÁLISIS"),
                extractSection(response, "PREDICCIÓN"),
                extractSection(response, "TIP")
        );
    }

    // ── Prompts ──────────────────────────────────────────────────

    private String buildUserAnalysisPrompt(Long userId, List<PredictionResponse> preds,
                                            RankingResponse ranking) {
        long settled   = preds.stream().filter(p -> Boolean.TRUE.equals(p.settled())).count();
        long correct   = preds.stream().filter(p -> Boolean.TRUE.equals(p.winnerCorrect())).count();
        int totalPts   = preds.stream().mapToInt(p -> p.totalPoints() != null ? p.totalPoints() : 0).sum();
        double accuracy = settled > 0 ? (double) correct / settled * 100 : 0;

        String predsSummary = preds.stream()
                .filter(p -> Boolean.TRUE.equals(p.settled()))
                .limit(5)
                .map(p -> String.format("partido %d: predijo %d-%d, puntos=%d",
                        p.matchId(), p.homeScore(), p.awayScore(), p.totalPoints()))
                .collect(Collectors.joining("; "));

        return String.format("""
                Eres un analista deportivo experto en el Mundial 2026.

                Datos del participante (userId=%d):
                - Posición en el ranking: %d
                - Puntos totales: %d
                - Predicciones realizadas: %d (%d liquidadas)
                - Aciertos de ganador: %d/%d (%.1f%% precisión)
                - Últimas predicciones: %s

                Responde EXACTAMENTE en este formato (sin texto adicional):
                ANÁLISIS: [análisis del rendimiento en 2-3 oraciones]
                PREDICCIÓN: [predicción de su tendencia para los próximos partidos]
                TIP: [un consejo específico y accionable para mejorar su puntaje]
                """,
                userId, ranking.position(), totalPts,
                preds.size(), settled, correct, settled, accuracy, predsSummary);
    }

    private String buildTournamentPrompt(List<RankingResponse> top10) {
        String rankStr = top10.stream()
                .map(r -> String.format("#%d %s (%s) — %.0f pts",
                        r.position(), r.username(), r.country(), r.points()))
                .collect(Collectors.joining("\n"));

        return String.format("""
                Eres un analista del torneo de predicciones del Mundial 2026.

                Top 10 del ranking global:
                %s

                Responde EXACTAMENTE en este formato:
                ANÁLISIS: [observaciones sobre el rendimiento del torneo en 2-3 oraciones]
                PREDICCIÓN: [quién tiene más probabilidades de ganar el torneo de predicciones]
                TIP: [consejo general para todos los participantes]
                """, rankStr);
    }

    // Extrae el contenido de una sección "CLAVE: contenido"
    private String extractSection(String text, String key) {
        if (text == null) return "";
        String marker = key + ":";
        int start = text.indexOf(marker);
        if (start == -1) return text; // si no encontró el formato, devuelve todo
        start += marker.length();
        int end = text.indexOf("\n", start);
        return end == -1 ? text.substring(start).trim() : text.substring(start, end).trim();
    }
}
