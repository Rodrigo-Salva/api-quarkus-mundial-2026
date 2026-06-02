package com.mundial2026.predictions.odds.service;

import com.mundial2026.predictions.matches.dto.MatchRequest;
import com.mundial2026.predictions.matches.service.MatchService;
import com.mundial2026.predictions.odds.dto.AddPlayerMarketRequest;
import com.mundial2026.predictions.odds.dto.OddsResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OddsServiceTest {

    @Inject OddsService oddsService;
    @Inject PlayerMarketService playerMarketService;
    @Inject MatchService matchService;

    private Long createMatch() {
        var m = matchService.create(new MatchRequest("A", "B", LocalDateTime.now().plusDays(1)));
        return m.id();
    }

    // ── Mercados estándar ──────────────────────────────────────

    @Test
    void testGetOddsForMatchEmpty() {
        var odds = oddsService.getOddsForMatch(99999L);
        assertNotNull(odds);
        assertTrue(odds.isEmpty());
    }

    @Test
    void testInitDefaultOddsCreatesAllMarkets() {
        Long mid = createMatch();
        oddsService.initDefaultOdds(mid);
        List<OddsResponse> odds = oddsService.getOddsForMatch(mid);
        // Debe tener todos los mercados del mapa DEFAULT_ODDS
        assertEquals(OddsService.DEFAULT_ODDS.size(), odds.size());
    }

    @Test
    void testNewMarketsArePresent() {
        Long mid = createMatch();
        oddsService.initDefaultOdds(mid);
        List<OddsResponse> odds = oddsService.getOddsForMatch(mid);
        List<String> markets = odds.stream().map(OddsResponse::market).toList();

        assertTrue(markets.contains("CORNERS_OVER_95"));
        assertTrue(markets.contains("CORNERS_UNDER_95"));
        assertTrue(markets.contains("CARDS_OVER_35"));
        assertTrue(markets.contains("CARDS_UNDER_35"));
        assertTrue(markets.contains("HOME_RED_CARD"));
        assertTrue(markets.contains("FIRST_HALF_HOME"));
        assertTrue(markets.contains("OVER_35"));
    }

    @Test
    void testCalculatePayoutCorrect() {
        BigDecimal payout = oddsService.calculatePayout(
                new BigDecimal("100.00"), new BigDecimal("2.10"));
        assertEquals(new BigDecimal("210.00"), payout);
    }

    @Test
    void testCalculateAccumulatorOdds() {
        BigDecimal result = oddsService.calculateAccumulatorOdds(List.of(
                new BigDecimal("2.00"),
                new BigDecimal("1.80"),
                new BigDecimal("3.00")
        ));
        assertEquals(new BigDecimal("10.80"), result);
    }

    @Test
    void testMaxPayoutCap() {
        BigDecimal payout = oddsService.calculateMaxPayout(
                new BigDecimal("1000.00"), new BigDecimal("200.00"));
        assertEquals(new BigDecimal("100000.00"), payout);
    }

    @Test
    void testInvalidMarket() {
        assertThrows(NotFoundException.class,
                () -> oddsService.getOdds(99999L, "INVALID_MARKET"));
    }

    // ── Cuotas dinámicas ──────────────────────────────────────

    @Test
    void testDynamicOddsNoChangeBeforeThreshold() {
        BigDecimal base = new BigDecimal("2.10");
        // Menos de 1000 PEN apostado → cuota no cambia
        BigDecimal adjusted = oddsService.calculateDynamicOdds(base, new BigDecimal("999.00"));
        assertEquals(base, adjusted);
    }

    @Test
    void testDynamicOddsDecreasesAfterThreshold() {
        BigDecimal base = new BigDecimal("2.10");
        // 1000 PEN apostado → 1 múltiplo → reducción 2%
        BigDecimal adjusted = oddsService.calculateDynamicOdds(base, new BigDecimal("1000.00"));
        assertTrue(adjusted.compareTo(base) < 0);
        assertEquals(new BigDecimal("2.06"), adjusted); // 2.10 * 0.98
    }

    @Test
    void testDynamicOddsMultipleThresholds() {
        BigDecimal base = new BigDecimal("2.10");
        // 3000 PEN → 3 múltiplos → reducción 6%
        BigDecimal adjusted = oddsService.calculateDynamicOdds(base, new BigDecimal("3000.00"));
        assertEquals(new BigDecimal("1.97"), adjusted); // 2.10 * 0.94
    }

    @Test
    void testDynamicOddsNeverBelowMinimum() {
        BigDecimal base = new BigDecimal("2.10");
        // Cantidad enorme → no puede bajar de 1.05
        BigDecimal adjusted = oddsService.calculateDynamicOdds(base, new BigDecimal("1000000.00"));
        assertEquals(new BigDecimal("1.05"), adjusted);
    }

    @Test
    void testRegisterBetAdjustsOddsInDb() {
        Long mid = createMatch();
        oddsService.initDefaultOdds(mid);

        // Apostar 3 veces 1000 PEN → totalStaked=3000 → 3 múltiplos → baja 6%
        oddsService.registerBetAndAdjustOdds(mid, "HOME_WIN", new BigDecimal("1000.00"));
        oddsService.registerBetAndAdjustOdds(mid, "HOME_WIN", new BigDecimal("1000.00"));
        oddsService.registerBetAndAdjustOdds(mid, "HOME_WIN", new BigDecimal("1000.00"));

        BigDecimal after = oddsService.getOdds(mid, "HOME_WIN").odds();
        // BASE 2.10, 3 múltiplos → 2.10 * 0.94 = 1.974 → 1.97
        assertTrue(after.compareTo(new BigDecimal("2.10")) < 0,
                "Expected odds < 2.10 but was: " + after);
    }

    // ── Mercados de jugador ────────────────────────────────────

    @Test
    void testAddPlayerMarket() {
        Long mid = createMatch();
        var res = playerMarketService.addPlayerMarket(mid,
                new AddPlayerMarketRequest("Messi", "FIRST_GOAL_SCORER", new BigDecimal("4.50")));

        assertNotNull(res.matchId());
        assertEquals("Messi", res.playerName());
        assertEquals("FIRST_GOAL_SCORER", res.market());
        assertEquals(new BigDecimal("4.50"), res.odds());
    }

    @Test
    void testGetPlayerMarketsByMatch() {
        Long mid = createMatch();
        playerMarketService.addPlayerMarket(mid,
                new AddPlayerMarketRequest("Ronaldo", "ANYTIME_SCORER", new BigDecimal("2.20")));
        playerMarketService.addPlayerMarket(mid,
                new AddPlayerMarketRequest("Mbappe", "ANYTIME_SCORER", new BigDecimal("2.50")));

        var list = playerMarketService.getByMatch(mid);
        assertEquals(2, list.size());
    }

    @Test
    void testPlayerMarketDynamicOddsAdjust() {
        Long mid = createMatch();
        playerMarketService.addPlayerMarket(mid,
                new AddPlayerMarketRequest("Neymar", "FIRST_GOAL_SCORER", new BigDecimal("5.00")));

        // 3 apuestas de 1000 PEN → totalStaked=3000 → baja 6%
        playerMarketService.registerBetAndAdjust(mid, "Neymar", "FIRST_GOAL_SCORER", new BigDecimal("1000.00"));
        playerMarketService.registerBetAndAdjust(mid, "Neymar", "FIRST_GOAL_SCORER", new BigDecimal("1000.00"));
        playerMarketService.registerBetAndAdjust(mid, "Neymar", "FIRST_GOAL_SCORER", new BigDecimal("1000.00"));

        var after = playerMarketService.getPlayerOdds(mid, "Neymar", "FIRST_GOAL_SCORER");
        // BASE 5.00, 3 múltiplos → 5.00 * 0.94 = 4.70
        assertTrue(after.odds().compareTo(new BigDecimal("5.00")) < 0,
                "Expected odds < 5.00 but was: " + after.odds());
    }

    @Test
    void testPlayerMarketNotFound() {
        assertThrows(NotFoundException.class,
                () -> playerMarketService.getPlayerOdds(99999L, "Ghost", "FIRST_GOAL_SCORER"));
    }
}
