package com.example.ironplan.rest.dto;

import com.example.ironplan.model.FreeActivityType;

import java.time.LocalDateTime;

public record FreeActivityResponse(
        Long id,
        FreeActivityType activityType,
        String activityTypeOther,
        Double distanceKm,
        Integer durationSeconds,
        String photoUrl,
        String notes,
        Integer caloriesEstimated,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {}
