package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.*;

public record CreateExerciseRequest(
        @NotNull(message = "El ID del ejercicio es requerido")
        Long exerciseId,

        @Size(max = 150)
        String displayName,

        @Min(1)
        int exerciseOrder,

        @Min(value = 1, message = "Mínimo 1 serie")
        @Max(value = 20, message = "Máximo 20 series")
        int sets,

        @Min(value = 1, message = "Mínimo 1 repetición")
        int repsMin,

        @Min(value = 1, message = "Mínimo 1 repetición")
        int repsMax,

        @Min(value = 0)
        @Max(value = 5)
        Integer rir,

        @Min(value = 0)
        Integer restMinutes
) {}

