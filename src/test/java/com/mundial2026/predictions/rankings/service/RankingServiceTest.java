package com.mundial2026.predictions.rankings.service;

import com.mundial2026.predictions.rankings.dto.RankingResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RankingServiceTest {

    @Inject
    RankingService rankingService;

    @Test
    void testAddAndGetGlobalRanking() {
        rankingService.addOrUpdateScore(1L, "player1", "ARG", 10.0);
        rankingService.addOrUpdateScore(2L, "player2", "BRA", 7.0);

        List<RankingResponse> ranking = rankingService.getGlobalRanking(10);
        assertFalse(ranking.isEmpty());
        assertEquals(1, ranking.get(0).position());
    }

    @Test
    void testCountryRanking() {
        rankingService.addOrUpdateScore(3L, "player3", "ESP", 5.0);
        List<RankingResponse> ranking = rankingService.getCountryRanking("ESP", 10);
        assertFalse(ranking.isEmpty());
    }

    @Test
    void testGetUserRankingNotFound() {
        RankingResponse ranking = rankingService.getUserRanking(99999L, "ghost", "ZZZ");
        assertEquals(-1, ranking.position());
    }
}
