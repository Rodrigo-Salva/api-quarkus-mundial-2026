package com.mundial2026.predictions.predictions.dto;

import com.mundial2026.predictions.predictions.entity.Prediction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta detallada con el desglose completo de puntos.
 * El usuario ve exactamente POR QUÉ ganó o perdió puntos.
 */
public record PredictionDetailResponse(
        Long              id,
        Long              matchId,
        String            homeTeam,
        String            awayTeam,

        // Lo que predijo
        Integer           predictedHome,
        Integer           predictedAway,

        // Resultado real (null si aún no terminó)
        Integer           actualHome,
        Integer           actualAway,

        String            matchStatus,    // SCHEDULED | LIVE | FINISHED
        LocalDateTime     matchDate,
        LocalDateTime     createdAt,
        Boolean           settled,

        // Desglose de puntos
        int               basePoints,     // 0, 2, 3 o 5
        int               earlyBonus,     // 0 o +1
        int               streakBonus,    // 0 o +2
        int               totalPoints,    // suma final
        String            result,         // EXACT | WINNER | DIFFERENCE | MISS | PENDING

        // Explicaciones legibles para mostrar en el front
        List<PointDetail> breakdown
) {

    /**
     * Detalle de un concepto de puntos:
     *  earned = true  → el usuario lo ganó   (icono ✅, color verde)
     *  earned = false → no lo ganó / no aplica (icono ❌ o gris)
     */
    public record PointDetail(
            String  concept,      // "Resultado exacto", "Ganador correcto", etc.
            String  description,  // explicación de por qué ganó o no
            int     points,       // cuántos puntos da este concepto
            boolean earned        // ¿lo ganó?
    ) {}

    public static PredictionDetailResponse from(
            Prediction p,
            String homeTeam, String awayTeam,
            Integer actualHome, Integer actualAway,
            String matchStatus, LocalDateTime matchDate) {

        List<PointDetail> breakdown = buildBreakdown(
                p, actualHome, actualAway, matchDate);

        String result = resolveResult(p, actualHome, actualAway);

        return new PredictionDetailResponse(
                p.id, p.matchId, homeTeam, awayTeam,
                p.homeScore, p.awayScore,
                actualHome, actualAway,
                matchStatus, matchDate, p.createdAt, p.settled,
                p.points        != null ? p.points        : 0,
                p.earlyBonus    != null ? p.earlyBonus    : 0,
                p.streakBonus   != null ? p.streakBonus   : 0,
                p.totalPoints   != null ? p.totalPoints   : 0,
                result,
                breakdown
        );
    }

    // ── Construye el desglose ─────────────────────────────────────
    private static List<PointDetail> buildBreakdown(
            Prediction p, Integer actualHome, Integer actualAway,
            LocalDateTime matchDate) {

        List<PointDetail> list = new ArrayList<>();

        if (!Boolean.TRUE.equals(p.settled)) {
            // Partido no terminado — mostrar solo el bonus anticipado
            int hours = matchDate != null && p.createdAt != null
                    ? (int) java.time.temporal.ChronoUnit.HOURS.between(
                            p.createdAt, matchDate)
                    : 0;
            boolean early = hours > 24;
            list.add(new PointDetail(
                    "Predicción anticipada",
                    early
                        ? "Predijiste " + hours + "h antes del partido (+1 pt)"
                        : "Predijiste con menos de 24h de anticipación (0 pts)",
                    1, early));
            list.add(new PointDetail(
                    "Resultado del partido",
                    "El partido aún no ha terminado",
                    0, false));
            return list;
        }

        // ── Regla 1: Resultado exacto ─────────────────────────────
        boolean exact = actualHome != null
                && p.homeScore.equals(actualHome)
                && p.awayScore.equals(actualAway);
        list.add(new PointDetail(
                "Resultado exacto",
                exact
                    ? "Predijiste " + p.homeScore + "-" + p.awayScore
                      + " y terminó " + actualHome + "-" + actualAway
                    : "Predijiste " + p.homeScore + "-" + p.awayScore
                      + " pero terminó " + actualHome + "-" + actualAway,
                5, exact));

        if (!exact && actualHome != null) {
            int predSign   = Integer.compare(p.homeScore, p.awayScore);
            int actualSign = Integer.compare(actualHome, actualAway);
            boolean sameWinner = predSign == actualSign;

            // ── Regla 2: Ganador correcto ─────────────────────────
            int predDiff   = Math.abs(p.homeScore - p.awayScore);
            int actualDiff = Math.abs(actualHome - actualAway);
            boolean sameDiff = sameWinner && predDiff == actualDiff;

            if (sameWinner && !sameDiff) {
                String winnerName = actualHome > actualAway ? "local"
                        : actualHome < actualAway ? "visitante" : "empate";
                list.add(new PointDetail(
                        "Ganador correcto",
                        "Acertaste que ganaría el " + winnerName
                          + " pero no el marcador exacto",
                        3, true));
            } else if (!sameWinner) {
                list.add(new PointDetail(
                        "Ganador correcto",
                        "No acertaste quién ganó el partido",
                        3, false));
            }

            // ── Regla 3: Diferencia de goles ─────────────────────
            if (sameWinner) {
                list.add(new PointDetail(
                        "Diferencia de goles correcta",
                        sameDiff
                            ? "Acertaste la diferencia de " + actualDiff + " goles"
                            : "La diferencia fue " + actualDiff
                              + " goles, predijiste " + predDiff,
                        2, sameDiff));
            } else {
                list.add(new PointDetail(
                        "Diferencia de goles correcta",
                        "No aplica porque no acertaste el ganador",
                        2, false));
            }
        }

        // ── Regla 5: Bonus anticipado ─────────────────────────────
        boolean early = Boolean.TRUE.equals(p.earlyBonus != null && p.earlyBonus > 0);
        int hoursAhead = matchDate != null && p.createdAt != null
                ? (int) java.time.temporal.ChronoUnit.HOURS.between(p.createdAt, matchDate)
                : 0;
        list.add(new PointDetail(
                "Predicción anticipada",
                early
                    ? "Predijiste " + hoursAhead + "h antes del partido (+1 pt)"
                    : "Predijiste con " + hoursAhead + "h de anticipación (necesitas >24h)",
                1, early));

        // ── Regla 4: Bonus racha ──────────────────────────────────
        boolean streak = p.streakBonus != null && p.streakBonus > 0;
        list.add(new PointDetail(
                "Bonus por racha",
                streak
                    ? "¡Completaste 3 partidos consecutivos acertando el ganador! (+2 pts)"
                    : "Aún no completas 3 partidos consecutivos acertados",
                2, streak));

        return list;
    }

    private static String resolveResult(Prediction p,
                                         Integer actualHome, Integer actualAway) {
        if (!Boolean.TRUE.equals(p.settled)) return "PENDING";
        if (actualHome == null)              return "PENDING";
        if (p.homeScore.equals(actualHome) && p.awayScore.equals(actualAway)) return "EXACT";
        int predSign   = Integer.compare(p.homeScore, p.awayScore);
        int actualSign = Integer.compare(actualHome,   actualAway);
        if (predSign != actualSign) return "MISS";
        int predDiff   = Math.abs(p.homeScore - p.awayScore);
        int actualDiff = Math.abs(actualHome   - actualAway);
        if (predDiff == actualDiff) return "DIFFERENCE";
        return "WINNER";
    }
}
