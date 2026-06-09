package com.mundial2026.predictions.leagues.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LeagueResponse(
        Long id,
        String name,
        String code,
        Long ownerId,
        LocalDateTime createdAt,
        String status,
        boolean isPrivate,
        List<MemberInfo> members
) {
    public record MemberInfo(Long userId, LocalDateTime joinedAt) {}
}
