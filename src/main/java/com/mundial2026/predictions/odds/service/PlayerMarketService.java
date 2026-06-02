package com.mundial2026.predictions.odds.service;

import com.mundial2026.predictions.odds.dto.AddPlayerMarketRequest;
import com.mundial2026.predictions.odds.dto.PlayerMarketResponse;
import com.mundial2026.predictions.odds.entity.PlayerMarket;
import com.mundial2026.predictions.odds.repository.PlayerMarketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class PlayerMarketService {

    @Inject
    PlayerMarketRepository playerMarketRepository;

    @Inject
    OddsService oddsService;

    public List<PlayerMarketResponse> getByMatch(Long matchId) {
        return playerMarketRepository.findByMatchId(matchId).stream()
                .map(PlayerMarketResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PlayerMarketResponse addPlayerMarket(Long matchId, AddPlayerMarketRequest req) {
        PlayerMarket pm = new PlayerMarket();
        pm.matchId    = matchId;
        pm.playerName = req.playerName();
        pm.market     = req.market();
        pm.odds       = req.odds();
        pm.baseOdds   = req.odds();
        pm.active     = true;
        playerMarketRepository.persist(pm);
        return PlayerMarketResponse.from(pm);
    }

    /**
     * Registra una apuesta en un mercado de jugador y ajusta la cuota dinámicamente.
     */
    @Transactional
    public void registerBetAndAdjust(Long matchId, String playerName, String market, BigDecimal stake) {
        playerMarketRepository.findByMatchIdPlayerAndMarket(matchId, playerName, market)
                .ifPresent(pm -> {
                    pm.totalStaked = pm.totalStaked.add(stake);
                    pm.betCount    = pm.betCount + 1;
                    pm.odds        = oddsService.calculateDynamicOdds(pm.baseOdds, pm.totalStaked);
                });
    }

    public PlayerMarketResponse getPlayerOdds(Long matchId, String playerName, String market) {
        return playerMarketRepository.findByMatchIdPlayerAndMarket(matchId, playerName, market)
                .map(PlayerMarketResponse::from)
                .orElseThrow(() -> new NotFoundException(
                        "No player market found: " + playerName + " / " + market));
    }
}
