package com.mundial2026.predictions.betting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AccumulatorBetRequest(
        @NotNull @Size(min = 2) List<Selection> selections,
        @NotNull @DecimalMin("1.00") BigDecimal stake
) {
    public record Selection(@NotNull Long matchId, @NotBlank String market) {}
}
