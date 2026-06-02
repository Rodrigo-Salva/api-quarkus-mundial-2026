package com.mundial2026.predictions.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DepositRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {}
