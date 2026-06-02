package com.mundial2026.predictions.wallet.repository;

import com.mundial2026.predictions.wallet.entity.Wallet;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@ApplicationScoped
public class WalletRepository implements PanacheRepository<Wallet> {

    public Optional<Wallet> findByUserId(Long userId) {
        return find("userId", userId).firstResultOptional();
    }

    public Optional<Wallet> findByUserIdForUpdate(Long userId) {
        return find("userId", userId)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }
}
