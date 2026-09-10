package com.example.ironplan.repository.projection;

import com.example.ironplan.model.FreeActivityType;

import java.time.LocalDateTime;

/** Actividad libre con identidad y evidencia, para historial y revisión admin. */
public record ActividadLibreDetalle(
        Long id,
        Long userId,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        FreeActivityType activityType,
        String activityTypeOther,
        Integer durationSeconds,
        Double distanceKm,
        String photoUrl
) {
}
