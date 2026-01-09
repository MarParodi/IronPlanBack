package com.example.ironplan.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateSessionRequest(
        @NotBlank(message = "El título de la sesión es requerido")
        @Size(max = 120, message = "El título no puede exceder 120 caracteres")
        String title,

        @Size(max = 16)
        String icon,

        @Size(max = 255)
        String muscles,

        @Size(max = 1000)
        String description,

        @Min(1)
        int sessionOrder,

        @Valid
        @NotEmpty(message = "La sesión debe tener al menos un ejercicio")
        List<CreateExerciseRequest> exercises
) {}
