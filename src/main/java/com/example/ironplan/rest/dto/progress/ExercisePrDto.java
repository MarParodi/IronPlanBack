package com.example.ironplan.rest.dto.progress;

/**
 * Personal Record de un ejercicio
 */
public record ExercisePrDto(
        Long exerciseId,
        String exerciseName,
        String primaryMuscle,
        Double topWeight,
        Integer topReps,
        Double estimated1RM,
        double totalVolumeKg
) {}
