package com.example.ironplan.rest.dto;

public record NextExerciseSummaryDto(
        Long workoutExerciseId,   // ID del ejercicio dentro de la sesión
        Integer exerciseOrder,    // orden actual dentro de la sesión

        String exerciseName,      // nombre del ejercicio ("Hiperextensiones prono en multipower")

        Integer plannedSets,      // 3 SERIES
        Integer plannedRepsMin,   // mínimo reps (10)
        Integer plannedRepsMax,   // máximo reps (12)
        Integer plannedRir,       // RIR (0, 1, 2)

        Long exerciseId,          // ID del ejercicio base del catálogo
        String exerciseVideoUrl   // video o imagen miniatura
) {}
