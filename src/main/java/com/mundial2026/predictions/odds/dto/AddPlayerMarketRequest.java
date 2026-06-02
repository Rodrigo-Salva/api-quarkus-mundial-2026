package com.mundial2026.predictions.odds.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AddPlayerMarketRequest(
        @NotBlank String playerName,
        @NotBlank String market,
        @NotNull @DecimalMin("1.05") BigDecimal odds
) {}
