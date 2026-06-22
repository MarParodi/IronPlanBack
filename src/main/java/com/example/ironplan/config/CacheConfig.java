package com.example.ironplan.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String LEADERBOARD = "leaderboard";
    public static final String MEMBER_LEADERBOARD = "memberLeaderboard";
    public static final String INTERNAL_RANKING = "internalRanking";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                LEADERBOARD, MEMBER_LEADERBOARD, INTERNAL_RANKING
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(200));
        return manager;
    }
}
