package com.mundial2026.predictions.shared.seed;

import com.mundial2026.predictions.auth.repository.UserRepository;
import com.mundial2026.predictions.odds.service.OddsService;
import com.mundial2026.predictions.matches.repository.MatchRepository;
import com.mundial2026.predictions.matches.entity.Match;
import com.mundial2026.predictions.rankings.service.RankingService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Inicializa datos en Redis al arrancar (solo en perfil dev/test).
 * Flyway ya insertó los datos en PostgreSQL (V12__seed_dev_data.sql).
 * Este bean sincroniza rankings y cuotas que viven en memoria.
 */
@ApplicationScoped
public class DevDataInitializer {

    private static final Logger LOG = Logger.getLogger(DevDataInitializer.class);

    @ConfigProperty(name = "quarkus.profile", defaultValue = "prod")
    String profile;

    @Inject UserRepository  userRepository;
    @Inject MatchRepository matchRepository;
    @Inject RankingService  rankingService;
    @Inject OddsService     oddsService;

    void onStart(@Observes StartupEvent event) {
        if ("prod".equals(profile)) return;

        LOG.info("=== DevDataInitializer: cargando datos de prueba en Redis ===");

        seedRankings();
        seedOddsForScheduledMatches();

        LOG.info("=== DevDataInitializer: listo ===");
    }

    // Carga los puntajes de los usuarios de prueba en Redis
    private void seedRankings() {
        try {
            // rodrigo26: 6 + 4 + 8 = 18 pts
            userRepository.findByUsername("rodrigo26").ifPresent(u ->
                    rankingService.addOrUpdateScore(u.id, u.username, u.country, 18));

            // carlitos10: 5 + 3 = 8 pts
            userRepository.findByUsername("carlitos10").ifPresent(u ->
                    rankingService.addOrUpdateScore(u.id, u.username, u.country, 8));

            // maria_gol: 6 pts
            userRepository.findByUsername("maria_gol").ifPresent(u ->
                    rankingService.addOrUpdateScore(u.id, u.username, u.country, 6));

            LOG.info("Rankings cargados en Redis");
        } catch (Exception e) {
            LOG.warn("No se pudo inicializar rankings en Redis: " + e.getMessage());
        }
    }

    // Inicializa cuotas para los partidos SCHEDULED que aún no tienen odds
    private void seedOddsForScheduledMatches() {
        try {
            matchRepository.findByStatus(Match.MatchStatus.SCHEDULED).forEach(match -> {
                oddsService.initDefaultOdds(match.id);
            });
            LOG.info("Cuotas inicializadas para partidos programados");
        } catch (Exception e) {
            LOG.warn("No se pudo inicializar cuotas: " + e.getMessage());
        }
    }
}
