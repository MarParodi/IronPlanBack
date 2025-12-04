package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WorkoutSetItemRequest(
        @NotNull @Min(1)
        Integer setNumber,

        @Min(0)
        Integer reps,

        @Min(0)
        Double weightKg,

        @NotNull
        Boolean completed
) {}
