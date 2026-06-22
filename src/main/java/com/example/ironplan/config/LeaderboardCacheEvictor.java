package com.example.ironplan.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardCacheEvictor {

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.LEADERBOARD, key = "#competitionId"),
            @CacheEvict(value = CacheConfig.MEMBER_LEADERBOARD, allEntries = true),
            @CacheEvict(value = CacheConfig.INTERNAL_RANKING, allEntries = true)
    })
    public void evictForCompetition(Long competitionId) {
        // La invalidación la gestionan las anotaciones.
    }
}
