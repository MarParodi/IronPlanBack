package com.example.ironplan.rest.dto;

import java.util.List;

public record WorkoutExerciseDetailDto(
        Long workoutExerciseId,
        Integer exerciseOrder,
        String exerciseName,
        Integer plannedSets,
        Integer plannedRepsMin,
        Integer plannedRepsMax,
        Integer plannedRir,
        Integer plannedRestSeconds,
        String status,
        Integer completedSets,
        List<WorkoutSetDetailDto> sets
) {}
