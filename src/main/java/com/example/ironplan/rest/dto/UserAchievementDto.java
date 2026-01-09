package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record UserAchievementDto(
        String code,
        String name,
        String description,
        String icon,
        Integer xpReward,
        LocalDateTime unlockedAt
) {}
