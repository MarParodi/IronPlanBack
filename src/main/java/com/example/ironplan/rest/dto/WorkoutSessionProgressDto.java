// WorkoutSessionProgressDto.java
package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record WorkoutSessionProgressDto(
        Long sessionId,
        int currentExerciseOrder,
        int totalExercises,
        double progressPercentage,
        int xpEarned,
        LocalDateTime createdAt
) {}
