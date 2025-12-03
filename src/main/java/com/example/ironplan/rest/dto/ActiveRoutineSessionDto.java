package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record ActiveRoutineSessionDto(
        Long sessionId,
        String title,
        Integer totalSeries,
        String mainMuscles,
        Integer orderInBlock,
        Boolean completed,
        LocalDateTime completedAt
) {}

