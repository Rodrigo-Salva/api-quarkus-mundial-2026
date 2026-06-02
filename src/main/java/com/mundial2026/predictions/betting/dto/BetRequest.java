package com.mundial2026.predictions.betting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BetRequest(
        @NotNull(message = "El ID del partido es obligatorio")
        @Positive(message = "El ID del partido debe ser un número positivo")
        Long matchId,

        @NotBlank(message = "El mercado de apuesta es obligatorio")
        @Pattern(
            regexp = "^(HOME_WIN|DRAW|AWAY_WIN|OVER_25|UNDER_25|OVER_35|UNDER_35"
                   + "|BOTH_SCORE_YES|BOTH_SCORE_NO"
                   + "|CORNERS_OVER_95|CORNERS_UNDER_95|CORNERS_OVER_115|CORNERS_UNDER_115"
                   + "|CARDS_OVER_35|CARDS_UNDER_35|HOME_RED_CARD|AWAY_RED_CARD"
                   + "|FIRST_HALF_HOME|FIRST_HALF_DRAW|FIRST_HALF_AWAY)$",
            message = "Mercado inválido. Consulta /api/odds/match/{id} para ver los mercados disponibles"
        )
        String market,

        @NotNull(message = "El monto de la apuesta es obligatorio")
        @DecimalMin(value = "1.00", message = "El monto mínimo de apuesta es 1.00 PEN")
        BigDecimal stake
) {}
