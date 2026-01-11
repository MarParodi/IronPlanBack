package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CustomWorkoutExerciseItem(
        @NotNull Long exerciseId,

        // Si viene null, el backend lo asigna 1..N
        @Min(1) Integer orderIndex,

        // Como tus columnas planned_* son NOT NULL, ponemos defaults si vienen null
        @Min(1) Integer plannedSets,
        @Min(1) Integer plannedRepsMin,
        @Min(1) Integer plannedRepsMax,

        @Min(0) Integer plannedRir,
        @Min(0) Integer plannedRestSeconds
) {}
