package com.mundial2026.predictions.tournaments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TournamentRequest(
    @NotBlank @Size(min = 3, max = 100) String name,
    @Size(max = 300) String description,
    boolean isPrivate
) {}
