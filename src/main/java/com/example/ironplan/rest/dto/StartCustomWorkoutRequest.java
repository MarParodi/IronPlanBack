package com.example.ironplan.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record StartCustomWorkoutRequest(
        String title,              // opcional: "Upper Body", "Pierna", etc.
        String notes,              // opcional
        LocalDateTime startedAt,   // opcional (si null, back usa now)
        @NotEmpty @Valid List<CustomWorkoutExerciseItem> exercises
) {}
