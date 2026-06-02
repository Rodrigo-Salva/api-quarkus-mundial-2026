package com.mundial2026.predictions.matches.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MatchRequest(
        @NotBlank String homeTeam,
        @NotBlank String awayTeam,
        @NotNull LocalDateTime matchDate
) {}
