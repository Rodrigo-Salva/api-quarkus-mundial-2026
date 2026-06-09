package com.mundial2026.predictions.tournaments.service;

import com.mundial2026.predictions.leagues.dto.LeagueRequest;
import com.mundial2026.predictions.leagues.service.LeagueService;
import com.mundial2026.predictions.shared.TestUserHelper;
import com.mundial2026.predictions.tournaments.dto.TournamentRequest;
import com.mundial2026.predictions.tournaments.dto.TournamentResponse;
import com.mundial2026.predictions.tournaments.dto.TournamentRankingEntry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TournamentServiceTest {

    @Inject TournamentService tournamentService;
    @Inject LeagueService     leagueService;
    @Inject TestUserHelper    users;

    // ── Crear torneo ──────────────────────────────────────────────

    @Test
    void testCreateTournament() {
        Long uid = users.createUser(9001);
        TournamentResponse t = tournamentService.create(
            new TournamentRequest("Copa Test", "Descripción de prueba", false), uid);

        assertNotNull(t.id());
        assertEquals("Copa Test", t.name());
        assertEquals("Descripción de prueba", t.description());
        assertEquals("OPEN", t.status());
        assertEquals(8, t.code().length());
        assertEquals(0, t.leagueCount());
    }

    @Test
    void testCreateTournamentGeneratesUniqueCode() {
        Long uid = users.createUser(9002);
        TournamentResponse t1 = tournamentService.create(new TournamentRequest("Torneo A", null, false), uid);
        TournamentResponse t2 = tournamentService.create(new TournamentRequest("Torneo B", null, false), uid);

        assertNotEquals(t1.code(), t2.code());
    }

    // ── Listar torneos ────────────────────────────────────────────

    @Test
    void testFindAll() {
        Long uid = users.createUser(9003);
        tournamentService.create(new TournamentRequest("Torneo Lista", null, false), uid);

        List<TournamentResponse> all = tournamentService.findAll();
        assertFalse(all.isEmpty());
    }

    @Test
    void testFindById() {
        Long uid = users.createUser(9004);
        TournamentResponse created = tournamentService.create(new TournamentRequest("Torneo ById", null, false), uid);

        TournamentResponse found = tournamentService.findById(created.id());
        assertEquals(created.id(), found.id());
        assertEquals("Torneo ById", found.name());
    }

    // ── Unir sala al torneo ───────────────────────────────────────

    @Test
    void testJoinLeagueToTournament() {
        Long owner   = users.createUser(9005);
        Long member  = users.createUser(9006);

        var league     = leagueService.create(owner, new LeagueRequest("Sala Join Test", false));
        var tournament = tournamentService.create(new TournamentRequest("Torneo Join", null, false), owner);

        TournamentResponse result = tournamentService.joinWithLeague(tournament.code(), league.id(), owner);

        assertEquals(1, result.leagueCount());
    }

    @Test
    void testJoinLeagueRequiresOwner() {
        Long owner   = users.createUser(9007);
        Long other   = users.createUser(9008);

        var league     = leagueService.create(owner, new LeagueRequest("Sala Owner Test", false));
        var tournament = tournamentService.create(new TournamentRequest("Torneo Owner", null, false), owner);

        // Solo el dueño de la sala puede inscribirla
        assertThrows(IllegalStateException.class,
            () -> tournamentService.joinWithLeague(tournament.code(), league.id(), other));
    }

    @Test
    void testJoinLeagueDuplicateFails() {
        Long uid = users.createUser(9009);
        var league     = leagueService.create(uid, new LeagueRequest("Sala Dup Torneo", false));
        var tournament = tournamentService.create(new TournamentRequest("Torneo Dup", null, false), uid);

        tournamentService.joinWithLeague(tournament.code(), league.id(), uid);

        assertThrows(IllegalStateException.class,
            () -> tournamentService.joinWithLeague(tournament.code(), league.id(), uid));
    }

    @Test
    void testJoinInvalidCodeFails() {
        Long uid   = users.createUser(9010);
        var league = leagueService.create(uid, new LeagueRequest("Sala Cod Invalid", false));

        assertThrows(jakarta.ws.rs.NotFoundException.class,
            () -> tournamentService.joinWithLeague("XXXXXXXX", league.id(), uid));
    }

    @Test
    void testCannotJoinFinishedTournament() {
        Long uid = users.createUser(9011);
        var league1    = leagueService.create(uid, new LeagueRequest("Sala Finished 1", false));
        var league2    = leagueService.create(users.createUser(9012), new LeagueRequest("Sala Finished 2", false));
        var tournament = tournamentService.create(new TournamentRequest("Torneo Finished", null, false), uid);

        tournamentService.updateStatus(tournament.id(), "FINISHED", uid);

        assertThrows(IllegalStateException.class,
            () -> tournamentService.joinWithLeague(tournament.code(), league2.id(), users.createUser(9012)));
    }

    // ── Ranking ───────────────────────────────────────────────────

    @Test
    void testRankingEmptyWhenNoLeagues() {
        Long uid = users.createUser(9013);
        var tournament = tournamentService.create(new TournamentRequest("Torneo Vacio", null, false), uid);

        List<TournamentRankingEntry> ranking = tournamentService.getRanking(tournament.id());
        assertTrue(ranking.isEmpty());
    }

    @Test
    void testRankingWithLeaguesAllZeroPoints() {
        Long uid1 = users.createUser(9014);
        Long uid2 = users.createUser(9015);

        var league1    = leagueService.create(uid1, new LeagueRequest("Sala Rank A", false));
        var league2    = leagueService.create(uid2, new LeagueRequest("Sala Rank B", false));
        var tournament = tournamentService.create(new TournamentRequest("Torneo Ranking Zero", null, false), uid1);

        tournamentService.joinWithLeague(tournament.code(), league1.id(), uid1);
        tournamentService.joinWithLeague(tournament.code(), league2.id(), uid2);

        List<TournamentRankingEntry> ranking = tournamentService.getRanking(tournament.id());

        assertEquals(2, ranking.size());
        // Sin predicciones liquidadas, todos tienen 0 puntos
        ranking.forEach(e -> assertEquals(0, e.totalPoints()));
        // Posiciones asignadas
        assertEquals(1, ranking.get(0).position());
        assertEquals(2, ranking.get(1).position());
        // Datos de sala presentes
        ranking.forEach(e -> {
            assertNotNull(e.leagueName());
            assertNotNull(e.ownerUsername());
            assertTrue(e.memberCount() >= 1);
        });
    }

    @Test
    void testRankingNotFoundThrows() {
        assertThrows(jakarta.ws.rs.NotFoundException.class,
            () -> tournamentService.getRanking(999999L));
    }

    // ── Cambio de estado ──────────────────────────────────────────

    @Test
    void testUpdateStatusByOwner() {
        Long uid = users.createUser(9016);
        var tournament = tournamentService.create(new TournamentRequest("Torneo Status", null, false), uid);

        TournamentResponse active = tournamentService.updateStatus(tournament.id(), "ACTIVE", uid);
        assertEquals("ACTIVE", active.status());

        TournamentResponse finished = tournamentService.updateStatus(tournament.id(), "FINISHED", uid);
        assertEquals("FINISHED", finished.status());
    }

    @Test
    void testUpdateStatusByNonOwnerFails() {
        Long owner = users.createUser(9017);
        Long other = users.createUser(9018);
        var tournament = tournamentService.create(new TournamentRequest("Torneo Status Guard", null, false), owner);

        assertThrows(IllegalStateException.class,
            () -> tournamentService.updateStatus(tournament.id(), "ACTIVE", other));
    }

    @Test
    void testUpdateStatusInvalidValueFails() {
        Long uid = users.createUser(9019);
        var tournament = tournamentService.create(new TournamentRequest("Torneo Bad Status", null, false), uid);

        assertThrows(IllegalArgumentException.class,
            () -> tournamentService.updateStatus(tournament.id(), "INVALID", uid));
    }
}
