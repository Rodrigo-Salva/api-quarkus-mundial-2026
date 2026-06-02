package com.mundial2026.predictions.wallet.service;

import com.mundial2026.predictions.kyc.service.KycService;
import com.mundial2026.predictions.wallet.dto.WalletResponse;
import com.mundial2026.predictions.wallet.entity.Wallet;
import com.mundial2026.predictions.wallet.entity.WalletTransaction;
import com.mundial2026.predictions.wallet.repository.TransactionRepository;
import com.mundial2026.predictions.wallet.repository.WalletRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class WalletService {

    @Inject
    WalletRepository walletRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    KycService kycService;

    @Transactional
    public Wallet getOrCreate(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.userId = userId;
            wallet.balance = BigDecimal.ZERO;
            wallet.currency = "PEN";
            walletRepository.persist(wallet);
            return wallet;
        });
    }

    public WalletResponse getBalance(Long userId) {
        return WalletResponse.from(getOrCreate(userId));
    }

    @Transactional
    public WalletResponse deposit(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del depósito debe ser mayor a cero");
        }
        kycService.checkDepositLimit(userId, amount);

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.userId = userId;
                    w.balance = BigDecimal.ZERO;
                    w.currency = "PEN";
                    walletRepository.persist(w);
                    return w;
                });

        BigDecimal before = wallet.balance;
        wallet.balance = wallet.balance.add(amount);
        recordTransaction(wallet, "DEPOSIT", amount, before, wallet.balance, null);
        return WalletResponse.from(wallet);
    }

    @Transactional
    public WalletResponse withdraw(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del retiro debe ser mayor a cero");
        }
        if (!kycService.isVerified(userId)) {
            throw new IllegalStateException("Debes completar la verificación de identidad (KYC) antes de retirar fondos");
        }
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("No tienes una billetera creada"));

        if (wallet.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente. Tu saldo actual es " + wallet.balance + " PEN");
        }
        BigDecimal before = wallet.balance;
        wallet.balance = wallet.balance.subtract(amount);
        recordTransaction(wallet, "WITHDRAWAL", amount, before, wallet.balance, null);
        return WalletResponse.from(wallet);
    }

    @Transactional
    public void debitBet(Long userId, BigDecimal amount, String reference) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("No tienes una billetera creada"));

        if (wallet.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente para realizar la apuesta. Tu saldo actual es " + wallet.balance + " PEN");
        }
        BigDecimal before = wallet.balance;
        wallet.balance = wallet.balance.subtract(amount);
        recordTransaction(wallet, "BET_PLACED", amount, before, wallet.balance, reference);
    }

    @Transactional
    public void creditWin(Long userId, BigDecimal amount, String reference) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> getOrCreate(userId));
        BigDecimal before = wallet.balance;
        wallet.balance = wallet.balance.add(amount);
        recordTransaction(wallet, "BET_WON", amount, before, wallet.balance, reference);
    }

    @Transactional
    public void refund(Long userId, BigDecimal amount, String reference) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> getOrCreate(userId));
        BigDecimal before = wallet.balance;
        wallet.balance = wallet.balance.add(amount);
        recordTransaction(wallet, "BET_REFUND", amount, before, wallet.balance, reference);
    }

    public List<WalletTransaction> getTransactions(Long userId) {
        Wallet wallet = getOrCreate(userId);
        return transactionRepository.findByWalletId(wallet.id);
    }

    private void recordTransaction(Wallet wallet, String type, BigDecimal amount,
                                   BigDecimal before, BigDecimal after, String reference) {
        WalletTransaction tx = new WalletTransaction();
        tx.walletId = wallet.id;
        tx.type = type;
        tx.amount = amount;
        tx.balanceBefore = before;
        tx.balanceAfter = after;
        tx.reference = reference;
        transactionRepository.persist(tx);
    }
}
