package com.mundial2026.predictions.odds.service;

import com.mundial2026.predictions.odds.dto.OddsResponse;
import com.mundial2026.predictions.odds.entity.OddsLine;
import com.mundial2026.predictions.odds.repository.OddsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class OddsService {

    private static final BigDecimal MAX_PAYOUT = new BigDecimal("100000.00");

    // Margen de la casa: 5%. Cuotas se ajustan dinámicamente hacia abajo cuando
    // el volumen de apuestas en un lado supera un umbral.
    private static final BigDecimal HOUSE_MARGIN    = new BigDecimal("0.05");
    private static final BigDecimal VOLUME_THRESHOLD = new BigDecimal("1000.00"); // PEN
    // Por cada múltiplo del umbral superado, la cuota baja un 2%
    private static final BigDecimal ADJUSTMENT_RATE = new BigDecimal("0.02");
    private static final BigDecimal MIN_ODDS        = new BigDecimal("1.05");

    static final Map<String, BigDecimal> DEFAULT_ODDS;
    static {
        DEFAULT_ODDS = new HashMap<>();
        // Resultado
        DEFAULT_ODDS.put("HOME_WIN",          new BigDecimal("2.10"));
        DEFAULT_ODDS.put("DRAW",              new BigDecimal("3.20"));
        DEFAULT_ODDS.put("AWAY_WIN",          new BigDecimal("3.50"));
        // Goles
        DEFAULT_ODDS.put("OVER_25",           new BigDecimal("1.80"));
        DEFAULT_ODDS.put("UNDER_25",          new BigDecimal("2.00"));
        DEFAULT_ODDS.put("OVER_35",           new BigDecimal("2.50"));
        DEFAULT_ODDS.put("UNDER_35",          new BigDecimal("1.55"));
        DEFAULT_ODDS.put("BOTH_SCORE_YES",    new BigDecimal("1.90"));
        DEFAULT_ODDS.put("BOTH_SCORE_NO",     new BigDecimal("1.85"));
        // Córners
        DEFAULT_ODDS.put("CORNERS_OVER_95",   new BigDecimal("1.90"));
        DEFAULT_ODDS.put("CORNERS_UNDER_95",  new BigDecimal("1.90"));
        DEFAULT_ODDS.put("CORNERS_OVER_115",  new BigDecimal("2.40"));
        DEFAULT_ODDS.put("CORNERS_UNDER_115", new BigDecimal("1.55"));
        // Tarjetas
        DEFAULT_ODDS.put("CARDS_OVER_35",     new BigDecimal("1.85"));
        DEFAULT_ODDS.put("CARDS_UNDER_35",    new BigDecimal("1.90"));
        DEFAULT_ODDS.put("HOME_RED_CARD",     new BigDecimal("4.50"));
        DEFAULT_ODDS.put("AWAY_RED_CARD",     new BigDecimal("4.50"));
        // Mitad
        DEFAULT_ODDS.put("FIRST_HALF_HOME",   new BigDecimal("3.00"));
        DEFAULT_ODDS.put("FIRST_HALF_DRAW",   new BigDecimal("2.10"));
        DEFAULT_ODDS.put("FIRST_HALF_AWAY",   new BigDecimal("4.50"));
    }

    @Inject
    OddsRepository oddsRepository;

    public List<OddsResponse> getOddsForMatch(Long matchId) {
        return oddsRepository.findByMatchId(matchId).stream()
                .map(OddsResponse::from)
                .collect(Collectors.toList());
    }

    public OddsResponse getOdds(Long matchId, String market) {
        return oddsRepository.findByMatchIdAndMarket(matchId, market)
                .map(OddsResponse::from)
                .orElseThrow(() -> new NotFoundException(
                        "No odds found for match " + matchId + " market " + market));
    }

    /**
     * Registra una nueva apuesta en el mercado y ajusta la cuota dinámicamente.
     * Cuanto más se apuesta a un lado, menor es la cuota (la casa balancea riesgo).
     */
    @Transactional
    public void registerBetAndAdjustOdds(Long matchId, String market, BigDecimal stake) {
        oddsRepository.findByMatchIdAndMarket(matchId, market).ifPresent(line -> {
            // Si base_odds no fue seteado, inicializarlo con la cuota actual
            if (line.baseOdds == null) line.baseOdds = line.odds;
            line.totalStaked = line.totalStaked.add(stake);
            line.betCount    = line.betCount + 1;
            line.odds        = calculateDynamicOdds(line.baseOdds, line.totalStaked);
        });
    }

    /**
     * Fórmula: odds = baseOdds * (1 - margin) * (1 - adjustmentRate * floor(totalStaked / threshold))
     * Resultado siempre >= MIN_ODDS.
     */
    BigDecimal calculateDynamicOdds(BigDecimal baseOdds, BigDecimal totalStaked) {
        if (baseOdds == null) return baseOdds;
        // Cuántos múltiplos del umbral se han superado
        int multiples = totalStaked.divide(VOLUME_THRESHOLD, 0, RoundingMode.FLOOR).intValue();
        if (multiples == 0) return baseOdds;

        BigDecimal reduction = ADJUSTMENT_RATE.multiply(new BigDecimal(multiples));
        BigDecimal factor    = BigDecimal.ONE.subtract(reduction);
        // No puede bajar de 0
        if (factor.compareTo(BigDecimal.ZERO) <= 0) return MIN_ODDS;

        BigDecimal adjusted = baseOdds.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        return adjusted.compareTo(MIN_ODDS) < 0 ? MIN_ODDS : adjusted;
    }

    public BigDecimal calculatePayout(BigDecimal stake, BigDecimal odds) {
        return stake.multiply(odds).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMaxPayout(BigDecimal stake, BigDecimal odds) {
        BigDecimal payout = calculatePayout(stake, odds);
        return payout.compareTo(MAX_PAYOUT) > 0 ? MAX_PAYOUT : payout;
    }

    public BigDecimal calculateAccumulatorOdds(List<BigDecimal> oddsList) {
        return oddsList.stream()
                .reduce(BigDecimal.ONE, BigDecimal::multiply)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void initDefaultOdds(Long matchId) {
        DEFAULT_ODDS.forEach((market, odds) -> {
            if (oddsRepository.findByMatchIdAndMarket(matchId, market).isEmpty()) {
                OddsLine line = new OddsLine();
                line.matchId   = matchId;
                line.market    = market;
                line.odds      = odds;
                line.baseOdds  = odds;
                line.active    = true;
                oddsRepository.persist(line);
            }
        });
    }
}
