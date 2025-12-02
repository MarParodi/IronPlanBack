// WorkoutSessionProgressDto.java
package com.example.ironplan.rest.dto;

public record WorkoutSessionProgressDto(
        Long sessionId,
        int currentExerciseOrder,
        int totalExercises,
        double progressPercentage,
        int xpEarned
) {}
