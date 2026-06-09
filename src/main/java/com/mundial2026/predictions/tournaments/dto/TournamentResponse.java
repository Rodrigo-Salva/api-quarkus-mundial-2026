package com.mundial2026.predictions.tournaments.dto;

import com.mundial2026.predictions.tournaments.entity.Tournament;

public record TournamentResponse(
    Long id,
    String name,
    String description,
    String code,
    Long ownerId,
    String ownerUsername,
    String status,
    boolean isPrivate,
    int leagueCount,
    String createdAt
) {
    public static TournamentResponse from(Tournament t, String ownerUsername, int leagueCount) {
        return new TournamentResponse(
            t.id, t.name, t.description, t.code,
            t.ownerId, ownerUsername, t.status, t.isPrivate, leagueCount,
            t.createdAt != null ? t.createdAt.toString() : null
        );
    }
}
