package com.mundial2026.predictions.matches.service;

import com.mundial2026.predictions.matches.dto.MatchRequest;
import com.mundial2026.predictions.matches.dto.MatchResponse;
import com.mundial2026.predictions.matches.entity.Match;
import com.mundial2026.predictions.matches.event.MatchEventProducer;
import com.mundial2026.predictions.matches.repository.MatchRepository;
import com.mundial2026.predictions.odds.service.OddsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class MatchService {

    @Inject MatchRepository     matchRepository;
    @Inject MatchEventProducer  matchEventProducer;
    @Inject SettlementService   settlementService;
    @Inject OddsService         oddsService;

    public List<MatchResponse> findAll() {
        return matchRepository.listAll().stream()
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }

    public MatchResponse findById(Long id) {
        Match match = matchRepository.findById(id);
        if (match == null) throw new NotFoundException("Partido no encontrado: " + id);
        return MatchResponse.from(match);
    }

    public List<MatchResponse> findLive() {
        return matchRepository.findLive().stream()
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MatchResponse create(MatchRequest req) {
        Match match = new Match();
        match.homeTeam  = req.homeTeam();
        match.awayTeam  = req.awayTeam();
        match.matchDate = req.matchDate();
        match.status    = Match.MatchStatus.SCHEDULED;
        matchRepository.persist(match);
        oddsService.initDefaultOdds(match.id);
        return MatchResponse.from(match);
    }

    @Transactional
    public MatchResponse updateResult(Long id, Integer homeScore, Integer awayScore, String status) {
        Match match = matchRepository.findById(id);
        if (match == null) throw new NotFoundException("Partido no encontrado: " + id);

        boolean justFinished = "FINISHED".equals(status)
                && match.status != Match.MatchStatus.FINISHED;

        match.homeScore = homeScore;
        match.awayScore = awayScore;
        match.status    = Match.MatchStatus.valueOf(status);

        // Publicar evento Kafka (tiempo real)
        matchEventProducer.publishMatchUpdated(id, status, homeScore, awayScore);

        // Liquidación automática solo cuando el partido pasa a FINISHED
        if (justFinished) {
            settlementService.settleMatch(id, homeScore, awayScore, match.matchDate);
        }

        return MatchResponse.from(match);
    }
}
