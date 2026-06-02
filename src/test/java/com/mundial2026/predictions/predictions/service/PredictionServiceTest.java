package com.mundial2026.predictions.predictions.service;

import com.mundial2026.predictions.leagues.dto.LeagueRequest;
import com.mundial2026.predictions.leagues.service.LeagueService;
import com.mundial2026.predictions.matches.dto.MatchRequest;
import com.mundial2026.predictions.matches.service.MatchService;
import com.mundial2026.predictions.predictions.dto.PredictionRequest;
import com.mundial2026.predictions.shared.TestUserHelper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PredictionServiceTest {

    @Inject PredictionService predictionService;
    @Inject LeagueService     leagueService;
    @Inject MatchService      matchService;
    @Inject TestUserHelper    users;

    // ── Regla 1: marcador exacto → 5 pts ────────────────────────

    @Test
    void testExactScoreFivePoints() {
        assertEquals(5, predictionService.computeBasePoints(2, 1, 2, 1));
    }

    @Test
    void testExactScoreDrawFivePoints() {
        assertEquals(5, predictionService.computeBasePoints(0, 0, 0, 0));
    }

    // ── Regla 2: ganador correcto → 3 pts ───────────────────────

    @Test
    void testCorrectWinnerThreePoints() {
        assertEquals(3, predictionService.computeBasePoints(3, 0, 2, 1));
    }

    @Test
    void testCorrectDrawThreePoints() {
        // predice 1-1, termina 2-2 → ganador (empate) correcto pero marcador distinto
        // y diferencia 0 == 0 → aplica regla 3 (2pts), NO regla 2
        assertEquals(2, predictionService.computeBasePoints(1, 1, 2, 2));
    }

    @Test
    void testWrongWinnerZeroPoints() {
        assertEquals(0, predictionService.computeBasePoints(1, 0, 0, 2));
    }

    // ── Regla 3: diferencia correcta → 2 pts ────────────────────

    @Test
    void testCorrectGoalDifferenceTwoPoints() {
        // predice 3-1 (diff=2), termina 2-0 (diff=2), mismo ganador → 2 pts
        assertEquals(2, predictionService.computeBasePoints(3, 1, 2, 0));
    }

    @Test
    void testCorrectGoalDifferenceDrawTwoPoints() {
        // predice 1-1 (diff=0, empate), termina 3-3 (diff=0, empate) → 2 pts
        assertEquals(2, predictionService.computeBasePoints(1, 1, 3, 3));
    }

    @Test
    void testSameWinnerDifferentDiff() {
        // predice 2-0 (diff=2), termina 1-0 (diff=1) → mismo ganador, diff distinta → 3 pts
        assertEquals(3, predictionService.computeBasePoints(2, 0, 1, 0));
    }

    // ── Regla 5: bonus anticipación ─────────────────────────────

    @Test
    void testEarlyBonusMoreThan24h() {
        LocalDateTime match    = LocalDateTime.now().plusDays(2);
        LocalDateTime prediction = LocalDateTime.now();
        assertEquals(1, predictionService.computeEarlyBonus(prediction, match));
    }

    @Test
    void testEarlyBonusExactly24h() {
        LocalDateTime match      = LocalDateTime.now().plusHours(24);
        LocalDateTime prediction = LocalDateTime.now();
        // Exactamente 24h → NO aplica (debe ser MÁS de 24h)
        assertEquals(0, predictionService.computeEarlyBonus(prediction, match));
    }

    @Test
    void testEarlyBonusLessThan24h() {
        LocalDateTime match      = LocalDateTime.now().plusHours(10);
        LocalDateTime prediction = LocalDateTime.now();
        assertEquals(0, predictionService.computeEarlyBonus(prediction, match));
    }

    @Test
    void testEarlyBonusNullSafe() {
        assertEquals(0, predictionService.computeEarlyBonus(null, LocalDateTime.now()));
        assertEquals(0, predictionService.computeEarlyBonus(LocalDateTime.now(), null));
    }

    // ── Regla 4: isWinnerCorrect (usado por racha) ───────────────

    @Test
    void testIsWinnerCorrectHome() {
        assertTrue(predictionService.isWinnerCorrect(2, 0, 3, 1));
    }

    @Test
    void testIsWinnerCorrectDraw() {
        assertTrue(predictionService.isWinnerCorrect(1, 1, 2, 2));
    }

    @Test
    void testIsWinnerCorrectWrong() {
        assertFalse(predictionService.isWinnerCorrect(1, 0, 0, 1));
    }

    // ── findByUser vacío ─────────────────────────────────────────

    @Test
    void testFindByUserEmpty() {
        var predictions = predictionService.findByUser(999999L);
        assertNotNull(predictions);
        assertTrue(predictions.isEmpty());
    }

    // ── Regla de sala: no se puede predecir sin estar en sala ─────

    @Test
    void testCannotPredictWithoutLeague() {
        Long uid   = users.createUser(9901);
        var match  = matchService.create(new MatchRequest(
                "TeamA", "TeamB", LocalDateTime.now().plusDays(1)));

        assertThrows(IllegalStateException.class,
                () -> predictionService.submit(uid,
                        new PredictionRequest(match.id(), 1, 0)),
                "Debe lanzar excepción si el usuario no está en ninguna sala");
    }

    @Test
    void testCanPredictAfterJoiningLeague() {
        Long uid   = users.createUser(9902);
        var match  = matchService.create(new MatchRequest(
                "TeamC", "TeamD", LocalDateTime.now().plusDays(1)));

        // Crear sala y unirse
        leagueService.create(uid, new LeagueRequest("Sala Prediccion Test"));

        // Ahora sí puede predecir
        var pred = predictionService.submit(uid,
                new PredictionRequest(match.id(), 2, 1));

        assertNotNull(pred.id());
        assertEquals(match.id(), pred.matchId());
    }
}
