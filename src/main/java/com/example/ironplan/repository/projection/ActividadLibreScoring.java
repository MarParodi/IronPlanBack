package com.example.ironplan.repository.projection;

import com.example.ironplan.model.FreeActivityType;

import java.time.LocalDateTime;

/** Datos mínimos de una actividad libre para el cálculo de {@code TEAM_POINTS}. */
public record ActividadLibreScoring(
        Long userId,
        LocalDateTime completedAt,
        FreeActivityType activityType,
        Integer durationSeconds,
        String photoUrl
) {
}
