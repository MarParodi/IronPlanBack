package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WorkoutSessionDetailResponse(
        Long sessionId,
        String routineName,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        long durationMinutes,
        int totalSeries,
        double totalWeightKg,
        Integer xpEarned,
        List<WorkoutExerciseDetailDto> exercises
) {}
