package com.example.ironplan.model;

public enum MetricType {
	SESSIONS,
    ACTIVE_MINUTES,
    WORKOUTS_COUNT,
    FREE_ACTIVITY_COUNT,
    FREE_ACTIVITY_KM,
    VOLUME_TOTAL,

    /**
     * Puntaje compuesto (constancia + esfuerzo + progreso + bonos de equipo).
     * No se acumula en {@code user_activities}: se calcula al vuelo desde las
     * sesiones de origen por {@code RetoPointsScoringService}.
     */
    TEAM_POINTS
}
