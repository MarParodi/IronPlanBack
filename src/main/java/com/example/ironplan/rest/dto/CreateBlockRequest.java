package com.example.ironplan.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateBlockRequest(
        @NotBlank(message = "El nombre del bloque es requerido")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String name,

        @Size(max = 2000)
        String description,

        @Min(value = 1, message = "El orden debe ser al menos 1")
        int orderIndex,

        @Min(value = 1, message = "La duración debe ser al menos 1 semana")
        int durationWeeks,

        @Valid
        @NotEmpty(message = "El bloque debe tener al menos una sesión")
        List<CreateSessionRequest> sessions
) {}

