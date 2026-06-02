package com.mundial2026.predictions.matches.service;

import com.mundial2026.predictions.matches.dto.MatchRequest;
import com.mundial2026.predictions.matches.dto.MatchResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MatchServiceTest {

    @Inject
    MatchService matchService;

    @Test
    void testCreateMatch() {
        MatchRequest req = new MatchRequest("Argentina", "Brazil",
                LocalDateTime.now().plusDays(1));
        MatchResponse match = matchService.create(req);

        assertNotNull(match.id());
        assertEquals("Argentina", match.homeTeam());
        assertEquals("Brazil", match.awayTeam());
        assertEquals("SCHEDULED", match.status());
    }

    @Test
    void testFindAll() {
        var matches = matchService.findAll();
        assertNotNull(matches);
    }
}
