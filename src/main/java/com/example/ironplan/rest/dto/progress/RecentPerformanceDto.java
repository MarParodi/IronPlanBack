package com.example.ironplan.rest.dto.progress;

import java.time.LocalDateTime;

/**
 * Rendimiento reciente de un ejercicio (para calcular recomendación)
 */
public record RecentPerformanceDto(
        LocalDateTime date,
        Double weightKg,
        Integer avgReps,           // Promedio de reps por serie
        int completedSets,
        boolean hitMaxReps,        // ¿Alcanzó repsMax en al menos una serie?
        boolean hitMinReps,        // ¿Alcanzó al menos repsMin en todas las series?
        double volumeKg
) {}
