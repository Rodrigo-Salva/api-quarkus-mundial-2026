package com.mundial2026.predictions.wallet.repository;

import com.mundial2026.predictions.wallet.entity.WalletTransaction;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<WalletTransaction> {

    public List<WalletTransaction> findByWalletId(Long walletId) {
        return list("walletId = ?1 order by createdAt desc", walletId);
    }
}
