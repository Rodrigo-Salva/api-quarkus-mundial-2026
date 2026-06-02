package com.mundial2026.predictions.kyc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LimitRequest(
        @NotBlank String limitType,
        @NotNull @DecimalMin("1.00") BigDecimal amount
) {}
