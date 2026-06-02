package com.mundial2026.predictions.matches.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MatchResultRequest(
        @NotNull(message = "El marcador local es obligatorio")
        @Min(value = 0, message = "El marcador local no puede ser negativo")
        @Max(value = 30, message = "El marcador local no puede superar 30")
        Integer homeScore,

        @NotNull(message = "El marcador visitante es obligatorio")
        @Min(value = 0, message = "El marcador visitante no puede ser negativo")
        @Max(value = 30, message = "El marcador visitante no puede superar 30")
        Integer awayScore,

        @NotBlank(message = "El estado del partido es obligatorio")
        @Pattern(regexp = "^(LIVE|FINISHED)$",
                message = "El estado debe ser LIVE o FINISHED")
        String status
) {}
