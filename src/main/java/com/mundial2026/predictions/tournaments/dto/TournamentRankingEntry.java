package com.mundial2026.predictions.tournaments.dto;

public record TournamentRankingEntry(
    int position,
    Long leagueId,
    String leagueName,
    String ownerUsername,
    int memberCount,
    int totalPoints
) {}
