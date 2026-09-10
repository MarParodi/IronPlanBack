package com.example.ironplan.repository.projection;

import java.time.LocalDateTime;

/** Sesión de fuerza con identidad, para historial y revisión admin. */
public record SesionFuerzaDetalle(
        Long id,
        Long userId,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Double progressPercentage,
        Integer completedExercises
) {
}
