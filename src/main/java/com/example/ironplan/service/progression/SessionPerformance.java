package com.example.ironplan.service.progression;

import java.time.LocalDateTime;

/**
 * Rendimiento agregado de una sesión pasada de un ejercicio, sin dependencias de JPA.
 * Las listas que llegan a {@link ProgressionPolicy} vienen ordenadas de más reciente a más antigua.
 *
 * @param weightKg peso medio de las series completadas, o null si no se registró carga
 *                 (ejercicios a peso corporal)
 * @param avgRir   RIR medio registrado, o null si el usuario no lo anotó
 */
public record SessionPerformance(
        LocalDateTime date,
        Double weightKg,
        int avgReps,
        int completedSets,
        int setsReachingMax,
        int setsBelowMin,
        Double avgRir,
        double volumeKg
) {

    public boolean reachedMax() {
        return setsReachingMax > 0;
    }

    public boolean allSetsReachedMax() {
        return completedSets > 0 && setsReachingMax == completedSets;
    }

    public boolean metMinimumInAllSets() {
        return setsBelowMin == 0;
    }
}
