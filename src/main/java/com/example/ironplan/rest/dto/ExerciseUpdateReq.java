package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExerciseUpdateReq(
        @NotBlank @Size(max = 100) String name,
        @NotBlank String description,
        @NotBlank String instructions,
        @NotBlank String primaryMuscle,
        @NotBlank String secondaryMuscle,
        @NotBlank String videoUrl
) {}