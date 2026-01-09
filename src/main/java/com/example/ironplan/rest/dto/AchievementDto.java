package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record AchievementDto(
        Long id,
        String code,
        String name,
        String description,
        String icon,
        String category,
        Integer xpReward,
        Boolean unlocked,
        LocalDateTime unlockedAt
) {}
