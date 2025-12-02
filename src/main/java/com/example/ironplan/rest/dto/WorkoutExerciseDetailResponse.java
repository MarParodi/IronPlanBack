// WorkoutExerciseDetailResponse.java
package com.example.ironplan.rest.dto;

public record WorkoutExerciseDetailResponse(
        Long sessionId,

        Long workoutExerciseId,
        Integer exerciseOrder,

        String exerciseName,
        Integer plannedSets,
        Integer plannedRepsMin,
        Integer plannedRepsMax,
        Integer plannedRir,
        Integer plannedRestSeconds,

        // datos del catálogo Exercise (para mostrar video, etc.)
        Long exerciseId,
        String exerciseVideoUrl,

        // última serie completada
        WorkoutPreviousSetDto previousSet,

        // progreso global de la sesión
        WorkoutSessionProgressDto progress
) {}
