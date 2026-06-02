package com.mundial2026.predictions.leagues.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeagueRequest(@NotBlank @Size(min = 3, max = 100) String name) {}
