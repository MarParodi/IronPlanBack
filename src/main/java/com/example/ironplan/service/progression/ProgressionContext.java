package com.example.ironplan.service.progression;

import java.util.List;

/**
 * Todo lo que {@link ProgressionPolicy} necesita para decidir, ya resuelto por quien consulta.
 *
 * @param recentSessions historial ordenado de más reciente a más antigua; puede venir vacío
 */
public record ProgressionContext(
        int repsMin,
        int repsMax,
        double weightIncrementKg,
        List<SessionPerformance> recentSessions
) {

    public ProgressionContext {
        recentSessions = recentSessions == null ? List.of() : List.copyOf(recentSessions);
    }

    public int repsMidpoint() {
        return (repsMin + repsMax) / 2;
    }
}
