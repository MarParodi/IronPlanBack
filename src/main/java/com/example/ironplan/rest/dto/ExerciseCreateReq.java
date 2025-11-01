package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExerciseCreateReq(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100)
        String name,

        @NotBlank(message = "La descripción es obligatoria.")
        String description,

        @NotBlank(message = "Las instrucciones son obligatorias.")
        String instructions,

        @NotBlank(message = "El músculo primario es obligatorio.")
        String primaryMuscle,

        @NotBlank(message = "El músculo secundario es obligatorio.")
        String secondaryMuscle,

        @NotBlank(message = "La URL del video es obligatoria.")
        String videoUrl
) {}