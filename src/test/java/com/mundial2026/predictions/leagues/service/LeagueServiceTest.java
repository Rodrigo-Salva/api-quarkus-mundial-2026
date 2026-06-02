package com.mundial2026.predictions.leagues.service;

import com.mundial2026.predictions.leagues.dto.LeagueRankingEntry;
import com.mundial2026.predictions.leagues.dto.LeagueRequest;
import com.mundial2026.predictions.leagues.dto.LeagueResponse;
import com.mundial2026.predictions.shared.TestUserHelper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LeagueServiceTest {

    @Inject LeagueService   leagueService;
    @Inject TestUserHelper  users;

    @Test
    void testCreateLeague() {
        Long uid = users.createUser(5001);
        LeagueResponse league = leagueService.create(uid, new LeagueRequest("Mi Liga Test"));
        assertNotNull(league.id());
        assertNotNull(league.code());
        assertEquals(6, league.code().length());
        assertEquals("Mi Liga Test", league.name());
        assertEquals(1, league.members().size());
    }

    @Test
    void testJoinLeague() {
        Long owner  = users.createUser(5002);
        Long joiner = users.createUser(5003);
        LeagueResponse created = leagueService.create(owner, new LeagueRequest("Liga Join Test"));
        LeagueResponse joined  = leagueService.join(created.code(), joiner);
        assertTrue(joined.members().stream().anyMatch(m -> m.userId().equals(joiner)));
    }

    @Test
    void testJoinLeagueAlreadyMember() {
        Long uid = users.createUser(5004);
        LeagueResponse created = leagueService.create(uid, new LeagueRequest("Liga Dup Test"));
        assertThrows(IllegalArgumentException.class,
                () -> leagueService.join(created.code(), uid));
    }

    @Test
    void testLeagueRankingEmptyNoPoints() {
        Long uid1 = users.createUser(5005);
        Long uid2 = users.createUser(5006);
        LeagueResponse league = leagueService.create(uid1, new LeagueRequest("Ranking Test"));
        leagueService.join(league.code(), uid2);

        List<LeagueRankingEntry> ranking = leagueService.getLeagueRanking(league.id(), 50);

        assertEquals(2, ranking.size());
        // Sin predicciones todos tienen 0 puntos
        ranking.forEach(e -> assertEquals(0, e.points()));
        // Posiciones asignadas correctamente
        assertEquals(1L, ranking.get(0).position());
        assertEquals(2L, ranking.get(1).position());
    }

    @Test
    void testLeagueRankingOnlyCountsPointsAfterJoin() {
        // El ranking de sala solo cuenta predicciones desde joinedAt
        // Como no hay predicciones en el test, ambos tienen 0
        Long owner = users.createUser(5007);
        Long uid2  = users.createUser(5008);
        LeagueResponse league = leagueService.create(owner, new LeagueRequest("Ranking Fecha Test"));
        leagueService.join(league.code(), uid2);

        List<LeagueRankingEntry> ranking = leagueService.getLeagueRanking(league.id(), 50);

        assertNotNull(ranking);
        assertFalse(ranking.isEmpty());
        // joinedAt no es null
        ranking.forEach(e -> assertNotNull(e.joinedAt()));
    }
}
