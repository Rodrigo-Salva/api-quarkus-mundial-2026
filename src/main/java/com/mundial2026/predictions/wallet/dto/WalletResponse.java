package com.mundial2026.predictions.wallet.dto;

import com.mundial2026.predictions.wallet.entity.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long userId,
        BigDecimal balance,
        String currency,
        LocalDateTime updatedAt
) {
    public static WalletResponse from(Wallet w) {
        return new WalletResponse(w.userId, w.balance, w.currency, w.updatedAt);
    }
}
