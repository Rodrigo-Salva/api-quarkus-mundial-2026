package com.mundial2026.predictions.realtime.service;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LiveMatchService {

    private static final String LIVE_KEY_PREFIX = "live:match:";
    private static final long TTL_SECONDS = 4 * 60 * 60L;

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, String> values() {
        return redisDataSource.value(String.class);
    }

    public void updateMatchState(Long matchId, String stateJson) {
        values().setex(LIVE_KEY_PREFIX + matchId, TTL_SECONDS, stateJson);
    }

    public String getMatchState(Long matchId) {
        return values().get(LIVE_KEY_PREFIX + matchId);
    }

    public void clearMatchState(Long matchId) {
        redisDataSource.key(String.class).del(LIVE_KEY_PREFIX + matchId);
    }
}
