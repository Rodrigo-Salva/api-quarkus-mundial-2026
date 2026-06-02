package com.mundial2026.predictions.rankings.service;

import com.mundial2026.predictions.rankings.dto.RankingResponse;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@ApplicationScoped
public class RankingService {

    private static final String GLOBAL_KEY = "ranking:global";
    private static final String COUNTRY_KEY_PREFIX = "ranking:country:";

    @Inject
    RedisDataSource redisDataSource;

    private SortedSetCommands<String, String> sortedSet() {
        return redisDataSource.sortedSet(String.class);
    }

    public void addOrUpdateScore(Long userId, String username, String country, double points) {
        String member = userId + ":" + username + ":" + country;
        sortedSet().zadd(GLOBAL_KEY, points, member);
        sortedSet().zadd(COUNTRY_KEY_PREFIX + country.toUpperCase(), points, member);
    }

    public List<RankingResponse> getGlobalRanking(int limit) {
        return buildRanking(GLOBAL_KEY, limit);
    }

    public List<RankingResponse> getCountryRanking(String countryCode, int limit) {
        return buildRanking(COUNTRY_KEY_PREFIX + countryCode.toUpperCase(), limit);
    }

    public RankingResponse getUserRanking(Long userId, String username, String country) {
        String member = userId + ":" + username + ":" + country;
        OptionalLong rank = sortedSet().zrevrank(GLOBAL_KEY, member);
        OptionalDouble score = sortedSet().zscore(GLOBAL_KEY, member);
        if (rank.isEmpty()) {
            return new RankingResponse(-1, username, 0, country);
        }
        return new RankingResponse(rank.getAsLong() + 1, username, score.orElse(0), country);
    }

    private List<RankingResponse> buildRanking(String key, int limit) {
        List<ScoredValue<String>> entries = sortedSet().zrangeWithScores(key, 0, limit - 1,
                new ZRangeArgs().rev());
        List<RankingResponse> result = new ArrayList<>();
        long position = 1;
        for (ScoredValue<String> entry : entries) {
            String[] parts = entry.value().split(":");
            String username = parts.length > 1 ? parts[1] : entry.value();
            String country = parts.length > 2 ? parts[2] : "";
            result.add(new RankingResponse(position++, username, entry.score(), country));
        }
        return result;
    }
}
