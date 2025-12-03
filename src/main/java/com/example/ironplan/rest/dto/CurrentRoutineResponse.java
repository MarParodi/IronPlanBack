package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record CurrentRoutineResponse(
        Long id,
        String name,
        String description,
        String goal,
        String suggestedLevel,
        Integer daysPerWeek,
        Integer durationWeeks,
        String img,
        LocalDateTime startedAt
) {}

