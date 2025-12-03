package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Goal;
import com.example.ironplan.model.Level;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateRoutineRequest(
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @NotBlank(message = "La descripción es requerida")
        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String description,

        @Size(max = 2000, message = "La descripción larga no puede exceder 2000 caracteres")
        String longDescription,

        @NotNull(message = "El objetivo es requerido")
        Goal goal,

        @NotNull(message = "El nivel es requerido")
        Level suggestedLevel,

        @Min(value = 1, message = "Mínimo 1 día por semana")
        @Max(value = 7, message = "Máximo 7 días por semana")
        int daysPerWeek,

        @Min(value = 1, message = "Mínimo 1 semana de duración")
        @Max(value = 52, message = "Máximo 52 semanas de duración")
        int durationWeeks,

        String img,

        boolean isPublic,

        @Valid
        @NotEmpty(message = "Debe incluir al menos una sesión")
        List<CreateSessionRequest> sessions
) {}

