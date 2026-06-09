package com.mundial2026.predictions.tournaments.service;

import com.mundial2026.predictions.auth.repository.UserRepository;
import com.mundial2026.predictions.leagues.entity.League;
import com.mundial2026.predictions.leagues.repository.LeagueRepository;
import com.mundial2026.predictions.tournaments.dto.TournamentRankingEntry;
import com.mundial2026.predictions.tournaments.dto.TournamentRequest;
import com.mundial2026.predictions.tournaments.dto.TournamentResponse;
import com.mundial2026.predictions.tournaments.entity.Tournament;
import com.mundial2026.predictions.tournaments.entity.TournamentLeague;
import com.mundial2026.predictions.tournaments.repository.TournamentLeagueRepository;
import com.mundial2026.predictions.tournaments.repository.TournamentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class TournamentService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    @Inject TournamentRepository         tournamentRepository;
    @Inject TournamentLeagueRepository   tlRepository;
    @Inject LeagueRepository             leagueRepository;
    @Inject UserRepository               userRepository;
    @Inject EntityManager                em;

    public List<TournamentResponse> findAll() {
        return tournamentRepository.listAll().stream()
            .filter(t -> !t.isPrivate)
            .map(t -> {
                int count = tlRepository.findByTournamentId(t.id).size();
                var owner = userRepository.findById(t.ownerId);
                return TournamentResponse.from(t, owner != null ? owner.username : "?", count);
            })
            .sorted(Comparator.comparing(TournamentResponse::createdAt).reversed())
            .toList();
    }

    public TournamentResponse findById(Long id) {
        Tournament t = tournamentRepository.findById(id);
        if (t == null) throw new NotFoundException("Torneo no encontrado: " + id);
        int count = tlRepository.findByTournamentId(id).size();
        var owner = userRepository.findById(t.ownerId);
        return TournamentResponse.from(t, owner != null ? owner.username : "?", count);
    }

    @Transactional
    public TournamentResponse create(TournamentRequest req, Long ownerId) {
        Tournament t = new Tournament();
        t.name        = req.name();
        t.description = req.description();
        t.ownerId     = ownerId;
        t.status      = "OPEN";
        t.isPrivate   = req.isPrivate();
        t.code        = generateUniqueCode();
        tournamentRepository.persist(t);
        var owner = userRepository.findById(ownerId);
        return TournamentResponse.from(t, owner != null ? owner.username : "?", 0);
    }

    // El dueño de una sala la une al torneo con el código del torneo
    @Transactional
    public TournamentResponse joinWithLeague(String tournamentCode, Long leagueId, Long requestingUserId) {
        Tournament t = tournamentRepository.findByCode(tournamentCode)
            .orElseThrow(() -> new NotFoundException("Torneo no encontrado con código: " + tournamentCode));

        if (!"OPEN".equals(t.status))
            throw new IllegalStateException("Este torneo ya no acepta nuevas salas");

        League league = leagueRepository.findById(leagueId);
        if (league == null) throw new NotFoundException("Sala no encontrada: " + leagueId);

        if (!league.ownerId.equals(requestingUserId))
            throw new IllegalStateException("Solo el dueño de la sala puede inscribirla en un torneo");

        if (tlRepository.isLeagueInTournament(t.id, leagueId))
            throw new IllegalStateException("Esta sala ya está inscrita en el torneo");

        TournamentLeague tl = new TournamentLeague();
        tl.tournamentId = t.id;
        tl.leagueId     = leagueId;
        tlRepository.persist(tl);

        int count = tlRepository.findByTournamentId(t.id).size();
        var owner = userRepository.findById(t.ownerId);
        return TournamentResponse.from(t, owner != null ? owner.username : "?", count);
    }

    @SuppressWarnings("unchecked")
    public List<TournamentRankingEntry> getRanking(Long tournamentId) {
        if (tournamentRepository.findById(tournamentId) == null)
            throw new NotFoundException("Torneo no encontrado: " + tournamentId);

        // Una sola query: suma total_points de predicciones liquidadas agrupado por sala
        String sql = """
            SELECT
                l.id           AS league_id,
                l.name         AS league_name,
                u.username     AS owner_username,
                COUNT(DISTINCT lm.user_id) AS member_count,
                COALESCE(SUM(CASE WHEN p.settled = true THEN p.total_points ELSE 0 END), 0) AS total_points
            FROM tournament_leagues tl
            JOIN leagues l        ON l.id  = tl.league_id
            JOIN users u          ON u.id  = l.owner_id
            LEFT JOIN league_members lm ON lm.league_id = l.id
            LEFT JOIN predictions p     ON p.user_id    = lm.user_id
            WHERE tl.tournament_id = :tid
            GROUP BY l.id, l.name, u.username
            ORDER BY total_points DESC
            """;

        var rows = em.createNativeQuery(sql)
                .setParameter("tid", tournamentId)
                .getResultList();

        List<TournamentRankingEntry> result = new ArrayList<>();
        int pos = 1;
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.add(new TournamentRankingEntry(
                pos++,
                ((Number) r[0]).longValue(),
                (String)  r[1],
                (String)  r[2],
                ((Number) r[3]).intValue(),
                ((Number) r[4]).intValue()
            ));
        }
        return result;
    }

    // Solo el creador puede cambiar el estado
    @Transactional
    public TournamentResponse updateStatus(Long tournamentId, String newStatus, Long requestingUserId) {
        Tournament t = tournamentRepository.findById(tournamentId);
        if (t == null) throw new NotFoundException("Torneo no encontrado: " + tournamentId);
        if (!t.ownerId.equals(requestingUserId))
            throw new IllegalStateException("Solo el creador puede modificar el torneo");
        if (!List.of("OPEN", "ACTIVE", "FINISHED").contains(newStatus))
            throw new IllegalArgumentException("Estado inválido: " + newStatus);
        t.status = newStatus;
        int count = tlRepository.findByTournamentId(t.id).size();
        var owner = userRepository.findById(t.ownerId);
        return TournamentResponse.from(t, owner != null ? owner.username : "?", count);
    }

    @Transactional
    public void removeLeague(Long tournamentId, Long leagueId, Long requestingUserId) {
        Tournament t = tournamentRepository.findById(tournamentId);
        if (t == null) throw new NotFoundException("Torneo no encontrado: " + tournamentId);
        if (!t.ownerId.equals(requestingUserId))
            throw new IllegalStateException("Solo el creador del torneo puede expulsar salas");
        if (!tlRepository.isLeagueInTournament(tournamentId, leagueId))
            throw new NotFoundException("Esa sala no está en este torneo");
        tlRepository.delete("tournamentId = ?1 AND leagueId = ?2", tournamentId, leagueId);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
            code = sb.toString();
        } while (tournamentRepository.findByCode(code).isPresent());
        return code;
    }
}
