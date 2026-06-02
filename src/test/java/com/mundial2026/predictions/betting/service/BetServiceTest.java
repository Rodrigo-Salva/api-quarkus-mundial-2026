package com.mundial2026.predictions.betting.service;

import com.mundial2026.predictions.betting.dto.AccumulatorBetRequest;
import com.mundial2026.predictions.betting.dto.BetRequest;
import com.mundial2026.predictions.betting.dto.BetResponse;
import com.mundial2026.predictions.kyc.dto.KycSubmitRequest;
import com.mundial2026.predictions.kyc.dto.SelfExclusionRequest;
import com.mundial2026.predictions.kyc.service.KycService;
import com.mundial2026.predictions.matches.dto.MatchRequest;
import com.mundial2026.predictions.matches.service.MatchService;
import com.mundial2026.predictions.odds.service.OddsService;
import com.mundial2026.predictions.shared.TestUserHelper;
import com.mundial2026.predictions.wallet.service.WalletService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BetServiceTest {

    @Inject BetService betService;
    @Inject KycService kycService;
    @Inject WalletService walletService;
    @Inject MatchService matchService;
    @Inject OddsService oddsService;
    @Inject TestUserHelper users;

    private Long matchId;

    @BeforeEach
    void setUp() {
        var match = matchService.create(new MatchRequest(
                "Spain", "France", LocalDateTime.now().plusDays(1)));
        matchId = match.id();
        oddsService.initDefaultOdds(matchId);
    }

    @Test
    void testPlaceBetSuccess() {
        Long uid = users.createUser(3001);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("500.00"));

        BetResponse res = betService.placeBet(uid,
                new BetRequest(matchId, "HOME_WIN", new BigDecimal("100.00")));

        assertNotNull(res.id());
        assertEquals("PENDING", res.status());
        assertEquals("HOME_WIN", res.market());
    }

    @Test
    void testPlaceBetKycRequired() {
        Long uid = users.createUser(3002);
        walletService.deposit(uid, new BigDecimal("100.00"));
        assertThrows(IllegalStateException.class,
                () -> betService.placeBet(uid,
                        new BetRequest(matchId, "HOME_WIN", new BigDecimal("10.00"))));
    }

    @Test
    void testPlaceBetExcluded() {
        Long uid = users.createUser(3003);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("100.00"));
        kycService.selfExclude(uid, new SelfExclusionRequest(null, "Test exclusion"));

        assertThrows(IllegalStateException.class,
                () -> betService.placeBet(uid,
                        new BetRequest(matchId, "DRAW", new BigDecimal("10.00"))));
    }

    @Test
    void testPlaceBetMatchFinished() {
        Long uid = users.createUser(3004);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("100.00"));

        var finishedMatch = matchService.create(new MatchRequest(
                "Brazil", "Germany", LocalDateTime.now().minusDays(1)));
        matchService.updateResult(finishedMatch.id(), 2, 1, "FINISHED");

        assertThrows(IllegalArgumentException.class,
                () -> betService.placeBet(uid,
                        new BetRequest(finishedMatch.id(), "HOME_WIN", new BigDecimal("10.00"))));
    }

    @Test
    void testPlaceBetInsufficientBalance() {
        Long uid = users.createUser(3005);
        approveKyc(uid);
        walletService.getOrCreate(uid);

        assertThrows(IllegalStateException.class,
                () -> betService.placeBet(uid,
                        new BetRequest(matchId, "HOME_WIN", new BigDecimal("500.00"))));
    }

    @Test
    void testSettleBetWon() {
        Long uid = users.createUser(3006);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("200.00"));

        betService.placeBet(uid, new BetRequest(matchId, "HOME_WIN", new BigDecimal("100.00")));
        betService.settleMatch(matchId, 2, 0);

        var balance = walletService.getBalance(uid);
        assertTrue(balance.balance().compareTo(new BigDecimal("100.00")) > 0);
    }

    @Test
    void testSettleBetLost() {
        Long uid = users.createUser(3007);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("200.00"));

        betService.placeBet(uid, new BetRequest(matchId, "HOME_WIN", new BigDecimal("100.00")));
        betService.settleMatch(matchId, 0, 2);

        var balance = walletService.getBalance(uid);
        assertEquals(0, new BigDecimal("100.00").compareTo(balance.balance()));
    }

    @Test
    void testCancelBet() {
        Long uid = users.createUser(3008);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("200.00"));

        BetResponse bet = betService.placeBet(uid,
                new BetRequest(matchId, "DRAW", new BigDecimal("50.00")));
        BetResponse cancelled = betService.cancelBet(bet.id(), uid);

        assertEquals("CANCELLED", cancelled.status());
        var balance = walletService.getBalance(uid);
        assertEquals(0, new BigDecimal("200.00").compareTo(balance.balance()));
    }

    @Test
    void testCancelBetAlreadySettled() {
        Long uid = users.createUser(3009);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("200.00"));

        BetResponse bet = betService.placeBet(uid,
                new BetRequest(matchId, "AWAY_WIN", new BigDecimal("50.00")));
        betService.settleMatch(matchId, 2, 0);

        assertThrows(IllegalStateException.class,
                () -> betService.cancelBet(bet.id(), uid));
    }

    @Test
    void testPlaceAccumulatorBet() {
        Long uid = users.createUser(3010);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("200.00"));

        var match2 = matchService.create(new MatchRequest(
                "Italy", "Portugal", LocalDateTime.now().plusDays(2)));
        oddsService.initDefaultOdds(match2.id());

        var req = new AccumulatorBetRequest(
                List.of(
                        new AccumulatorBetRequest.Selection(matchId, "HOME_WIN"),
                        new AccumulatorBetRequest.Selection(match2.id(), "DRAW")
                ),
                new BigDecimal("50.00")
        );
        var res = betService.placeAccumulatorBet(uid, req);

        assertNotNull(res.id());
        assertEquals("PENDING", res.status());
        assertTrue(res.totalOdds().compareTo(BigDecimal.ONE) > 0);
    }

    private void approveKyc(Long userId) {
        try {
            kycService.submit(userId, new KycSubmitRequest("DNI", "DOC-" + userId));
        } catch (IllegalArgumentException ignored) {}
        kycService.approve(userId);
    }
}
