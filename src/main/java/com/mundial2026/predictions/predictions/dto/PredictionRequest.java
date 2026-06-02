package com.mundial2026.predictions.predictions.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PredictionRequest(
        @NotNull(message = "El ID del partido es obligatorio")
        @Positive(message = "El ID del partido debe ser un número positivo")
        Long matchId,

        @NotNull(message = "El marcador local es obligatorio")
        @Min(value = 0, message = "El marcador local no puede ser negativo")
        @Max(value = 30, message = "El marcador local no puede superar 30")
        Integer homeScore,

        @NotNull(message = "El marcador visitante es obligatorio")
        @Min(value = 0, message = "El marcador visitante no puede ser negativo")
        @Max(value = 30, message = "El marcador visitante no puede superar 30")
        Integer awayScore
) {}
