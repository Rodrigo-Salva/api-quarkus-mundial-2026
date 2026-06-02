package com.mundial2026.predictions.wallet.service;

import com.mundial2026.predictions.kyc.dto.KycSubmitRequest;
import com.mundial2026.predictions.kyc.service.KycService;
import com.mundial2026.predictions.shared.TestUserHelper;
import com.mundial2026.predictions.wallet.entity.WalletTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WalletServiceTest {

    @Inject WalletService walletService;
    @Inject KycService kycService;
    @Inject TestUserHelper users;

    @Test
    void testGetOrCreateWallet() {
        Long uid = users.createUser(2001);
        var wallet = walletService.getOrCreate(uid);
        assertNotNull(wallet);
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.balance));
        assertEquals("PEN", wallet.currency);
    }

    @Test
    void testDeposit() {
        Long uid = users.createUser(2002);
        var res = walletService.deposit(uid, new BigDecimal("500.00"));
        assertEquals(0, new BigDecimal("500.00").compareTo(res.balance()));
    }

    @Test
    void testWithdrawSuccess() {
        Long uid = users.createUser(2003);
        approveKyc(uid);
        walletService.deposit(uid, new BigDecimal("300.00"));
        var res = walletService.withdraw(uid, new BigDecimal("100.00"));
        assertEquals(0, new BigDecimal("200.00").compareTo(res.balance()));
    }

    @Test
    void testWithdrawInsufficientBalance() {
        Long uid = users.createUser(2004);
        approveKyc(uid);
        walletService.getOrCreate(uid);
        assertThrows(IllegalStateException.class,
                () -> walletService.withdraw(uid, new BigDecimal("999.00")));
    }

    @Test
    void testDebitBet() {
        Long uid = users.createUser(2005);
        walletService.deposit(uid, new BigDecimal("200.00"));
        walletService.debitBet(uid, new BigDecimal("50.00"), "BET-123");
        var balance = walletService.getBalance(uid);
        assertEquals(0, new BigDecimal("150.00").compareTo(balance.balance()));
    }

    @Test
    void testCreditWin() {
        Long uid = users.createUser(2006);
        walletService.getOrCreate(uid);
        walletService.creditWin(uid, new BigDecimal("210.00"), "BET-WIN-1");
        var balance = walletService.getBalance(uid);
        assertEquals(0, new BigDecimal("210.00").compareTo(balance.balance()));
    }

    @Test
    void testRefund() {
        Long uid = users.createUser(2007);
        walletService.deposit(uid, new BigDecimal("100.00"));
        walletService.debitBet(uid, new BigDecimal("100.00"), "BET-456");
        walletService.refund(uid, new BigDecimal("100.00"), "BET-CANCEL-456");
        var balance = walletService.getBalance(uid);
        assertEquals(0, new BigDecimal("100.00").compareTo(balance.balance()));
    }

    @Test
    void testGetTransactionHistory() {
        Long uid = users.createUser(2008);
        walletService.deposit(uid, new BigDecimal("100.00"));
        walletService.deposit(uid, new BigDecimal("50.00"));
        List<WalletTransaction> txs = walletService.getTransactions(uid);
        assertTrue(txs.size() >= 2);
    }

    @Test
    void testDepositNegativeAmount() {
        Long uid = users.createUser(2009);
        assertThrows(IllegalArgumentException.class,
                () -> walletService.deposit(uid, new BigDecimal("-10.00")));
    }

    @Test
    void testWithdrawZeroAmount() {
        Long uid = users.createUser(2010);
        assertThrows(IllegalArgumentException.class,
                () -> walletService.withdraw(uid, BigDecimal.ZERO));
    }

    private void approveKyc(Long uid) {
        try { kycService.submit(uid, new KycSubmitRequest("DNI", "DOC-" + uid)); }
        catch (IllegalArgumentException ignored) {}
        kycService.approve(uid);
    }
}
