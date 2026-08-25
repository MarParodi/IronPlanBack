package com.example.ironplan.repository.projection;

import java.time.LocalDateTime;

/** Datos mínimos de una sesión de fuerza para el cálculo de {@code TEAM_POINTS}. */
public record SesionFuerzaScoring(
        Long userId,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Double progressPercentage,
        Integer completedExercises
) {
}
