package com.mundial2026.predictions.betting.service;

import com.mundial2026.predictions.betting.dto.AccumulatorBetRequest;
import com.mundial2026.predictions.betting.dto.AccumulatorBetResponse;
import com.mundial2026.predictions.betting.dto.BetRequest;
import com.mundial2026.predictions.betting.dto.BetResponse;
import com.mundial2026.predictions.betting.entity.Bet;
import com.mundial2026.predictions.betting.entity.AccumulatorBet;
import com.mundial2026.predictions.betting.entity.AccumulatorBetSelection;
import com.mundial2026.predictions.betting.event.BetEventProducer;
import com.mundial2026.predictions.betting.repository.AccumulatorBetRepository;
import com.mundial2026.predictions.betting.repository.AccumulatorBetSelectionRepository;
import com.mundial2026.predictions.betting.repository.BetRepository;
import com.mundial2026.predictions.kyc.service.KycService;
import com.mundial2026.predictions.matches.service.MatchService;
import com.mundial2026.predictions.odds.dto.OddsResponse;
import com.mundial2026.predictions.odds.service.OddsService;
import com.mundial2026.predictions.wallet.service.WalletService;
import com.mundial2026.predictions.odds.service.PlayerMarketService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BetService {

    @Inject BetRepository betRepository;
    @Inject AccumulatorBetRepository accumulatorBetRepository;
    @Inject AccumulatorBetSelectionRepository selectionRepository;
    @Inject KycService kycService;
    @Inject PlayerMarketService playerMarketService;
    @Inject OddsService oddsService;
    @Inject WalletService walletService;
    @Inject MatchService matchService;
    @Inject BetEventProducer betEventProducer;

    @Transactional
    public BetResponse placeBet(Long userId, BetRequest req) {
        validateUser(userId);

        var match = matchService.findById(req.matchId());
        if ("FINISHED".equals(match.status())) {
            throw new IllegalArgumentException("Cannot bet on a finished match");
        }

        OddsResponse oddsLine = oddsService.getOdds(req.matchId(), req.market());
        BigDecimal potentialPayout = oddsService.calculateMaxPayout(req.stake(), oddsLine.odds());

        Bet bet = new Bet();
        bet.userId = userId;
        bet.matchId = req.matchId();
        bet.market = req.market();
        bet.stake = req.stake();
        bet.odds = oddsLine.odds();
        bet.potentialPayout = potentialPayout;
        bet.status = "PENDING";
        betRepository.persist(bet);

        walletService.debitBet(userId, req.stake(), "BET-" + bet.id);
        // Ajustar cuota dinámicamente tras registrar la apuesta
        oddsService.registerBetAndAdjustOdds(req.matchId(), req.market(), req.stake());
        betEventProducer.publishBetPlaced(bet);

        return BetResponse.from(bet);
    }

    @Transactional
    public AccumulatorBetResponse placeAccumulatorBet(Long userId, AccumulatorBetRequest req) {
        validateUser(userId);

        List<BigDecimal> allOdds = new ArrayList<>();
        List<AccumulatorBetSelection> selections = new ArrayList<>();

        for (AccumulatorBetRequest.Selection sel : req.selections()) {
            var match = matchService.findById(sel.matchId());
            if ("FINISHED".equals(match.status())) {
                throw new IllegalArgumentException("Cannot include finished match in accumulator");
            }
            OddsResponse oddsLine = oddsService.getOdds(sel.matchId(), sel.market());
            allOdds.add(oddsLine.odds());

            AccumulatorBetSelection s = new AccumulatorBetSelection();
            s.matchId = sel.matchId();
            s.market = sel.market();
            s.odds = oddsLine.odds();
            s.result = "PENDING";
            selections.add(s);
        }

        BigDecimal totalOdds = oddsService.calculateAccumulatorOdds(allOdds);
        BigDecimal potentialPayout = oddsService.calculateMaxPayout(req.stake(), totalOdds);

        AccumulatorBet accBet = new AccumulatorBet();
        accBet.userId = userId;
        accBet.totalStake = req.stake();
        accBet.totalOdds = totalOdds;
        accBet.potentialPayout = potentialPayout;
        accBet.status = "PENDING";
        accumulatorBetRepository.persist(accBet);

        for (AccumulatorBetSelection s : selections) {
            s.accumulatorBetId = accBet.id;
            selectionRepository.persist(s);
        }

        walletService.debitBet(userId, req.stake(), "ACCU-" + accBet.id);
        return AccumulatorBetResponse.from(accBet);
    }

    @Transactional
    public void settleMatch(Long matchId, Integer homeScore, Integer awayScore) {
        List<Bet> bets = betRepository.findPendingByMatchId(matchId);
        for (Bet bet : bets) {
            boolean won = evaluateMarket(bet.market, homeScore, awayScore);
            bet.status = won ? "WON" : "LOST";
            bet.settledAt = LocalDateTime.now();
            if (won) {
                walletService.creditWin(bet.userId, bet.potentialPayout, "BET-WIN-" + bet.id);
            }
        }
    }

    @Transactional
    public BetResponse cancelBet(Long betId, Long userId) {
        Bet bet = betRepository.findById(betId);
        if (bet == null) throw new NotFoundException("Bet not found: " + betId);
        if (!bet.userId.equals(userId)) throw new SecurityException("Not your bet");
        if (!"PENDING".equals(bet.status)) {
            throw new IllegalStateException("No puedes cancelar una apuesta que ya fue liquidada");
        }
        var match = matchService.findById(bet.matchId);
        if (!"SCHEDULED".equals(match.status())) {
            throw new IllegalStateException("No puedes cancelar una apuesta de un partido que ya comenzó");
        }
        bet.status = "CANCELLED";
        bet.settledAt = LocalDateTime.now();
        walletService.refund(userId, bet.stake, "BET-CANCEL-" + bet.id);
        return BetResponse.from(bet);
    }

    public List<BetResponse> findByUser(Long userId) {
        return betRepository.findByUserId(userId).stream()
                .map(BetResponse::from)
                .collect(Collectors.toList());
    }

    public BetResponse findById(Long betId) {
        Bet bet = betRepository.findById(betId);
        if (bet == null) throw new NotFoundException("Bet not found: " + betId);
        return BetResponse.from(bet);
    }

    private void validateUser(Long userId) {
        if (!kycService.isVerified(userId)) {
            throw new IllegalStateException("Debes completar la verificación de identidad (KYC) antes de apostar");
        }
        if (kycService.isExcluded(userId)) {
            throw new IllegalStateException("Tu cuenta tiene una auto-exclusión activa. No puedes realizar apuestas");
        }
    }

    boolean evaluateMarket(String market, int home, int away) {
        return switch (market) {
            case "HOME_WIN"       -> home > away;
            case "DRAW"           -> home == away;
            case "AWAY_WIN"       -> away > home;
            case "OVER_25"        -> (home + away) > 2;
            case "UNDER_25"       -> (home + away) < 3;
            case "BOTH_SCORE_YES" -> home > 0 && away > 0;
            case "BOTH_SCORE_NO"  -> home == 0 || away == 0;
            default -> false;
        };
    }
}
