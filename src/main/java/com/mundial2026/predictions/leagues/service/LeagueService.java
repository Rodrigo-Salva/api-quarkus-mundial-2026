package com.mundial2026.predictions.leagues.service;

import com.mundial2026.predictions.auth.repository.UserRepository;
import com.mundial2026.predictions.leagues.dto.LeagueRequest;
import com.mundial2026.predictions.leagues.dto.LeagueResponse;
import com.mundial2026.predictions.leagues.dto.LeagueRankingEntry;
import com.mundial2026.predictions.leagues.entity.League;
import com.mundial2026.predictions.leagues.entity.LeagueMember;
import com.mundial2026.predictions.leagues.repository.LeagueMemberRepository;
import com.mundial2026.predictions.leagues.repository.LeagueRepository;
import com.mundial2026.predictions.predictions.repository.PredictionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@ApplicationScoped
public class LeagueService {

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int    CODE_LENGTH = 6;

    @Inject LeagueRepository       leagueRepository;
    @Inject LeagueMemberRepository leagueMemberRepository;
    @Inject PredictionRepository   predictionRepository;
    @Inject UserRepository         userRepository;

    @Transactional
    public LeagueResponse create(Long ownerId, LeagueRequest req) {
        League league = new League();
        league.name    = req.name();
        league.code    = generateUniqueCode();
        league.ownerId = ownerId;
        leagueRepository.persist(league);

        LeagueMember member = new LeagueMember();
        member.leagueId = league.id;
        member.userId   = ownerId;
        leagueMemberRepository.persist(member);

        return buildResponse(league, List.of(member));
    }

    @Transactional
    public LeagueResponse join(String code, Long userId) {
        League league = leagueRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException(
                        "No existe ninguna sala con el código: " + code));

        if (leagueMemberRepository.isMember(league.id, userId)) {
            throw new IllegalArgumentException("Ya eres miembro de esta sala");
        }

        LeagueMember member = new LeagueMember();
        member.leagueId = league.id;
        member.userId   = userId;
        leagueMemberRepository.persist(member);

        List<LeagueMember> members = leagueMemberRepository.findByLeagueId(league.id);
        return buildResponse(league, members);
    }

    public LeagueResponse findById(Long id) {
        League league = leagueRepository.findById(id);
        if (league == null) throw new NotFoundException("Sala no encontrada: " + id);
        List<LeagueMember> members = leagueMemberRepository.findByLeagueId(id);
        return buildResponse(league, members);
    }

    /**
     * Ranking real de la sala:
     * - Solo cuenta puntos de predicciones hechas DESDE que el usuario se unió
     * - Ordenado de mayor a menor puntaje
     * - Incluye posición, username, puntos y país
     */
    public List<LeagueRankingEntry> getLeagueRanking(Long leagueId, int limit) {
        League league = leagueRepository.findById(leagueId);
        if (league == null) throw new NotFoundException("Sala no encontrada: " + leagueId);

        List<LeagueMember> members = leagueMemberRepository.findByLeagueIdOrdered(leagueId);

        AtomicLong position = new AtomicLong(1);

        return members.stream()
                .map(member -> {
                    // Puntos solo desde joinedAt
                    int points = predictionRepository.sumPointsByUserSince(
                            member.userId, member.joinedAt);

                    var userOpt = userRepository.findByIdOptional(member.userId);
                    String username = userOpt.map(u -> u.username).orElse("usuario_" + member.userId);
                    String country  = userOpt.map(u -> u.country).orElse("??");

                    return new LeagueRankingEntry(
                            0L, member.userId, username, country, points, member.joinedAt);
                })
                .sorted(Comparator.comparingInt(LeagueRankingEntry::points).reversed())
                .limit(limit)
                .map(entry -> new LeagueRankingEntry(
                        position.getAndIncrement(),
                        entry.userId(), entry.username(), entry.country(),
                        entry.points(), entry.joinedAt()))
                .collect(Collectors.toList());
    }

    private String generateUniqueCode() {
        SecureRandom random = new SecureRandom();
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (leagueRepository.findByCode(code).isPresent());
        return code;
    }

    private LeagueResponse buildResponse(League league, List<LeagueMember> members) {
        List<LeagueResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> new LeagueResponse.MemberInfo(m.userId, m.joinedAt))
                .collect(Collectors.toList());
        return new LeagueResponse(league.id, league.name, league.code,
                league.ownerId, league.createdAt, memberInfos);
    }
}
