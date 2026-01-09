package com.example.ironplan.rest.dto.progress;

import java.time.LocalDateTime;

/**
 * Representa el mejor set (por peso) de un ejercicio
 */
public record TopSetDto(
        Double weightKg,
        Integer reps,
        LocalDateTime date
) {}
