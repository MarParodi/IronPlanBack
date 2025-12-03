// WorkoutSessionSummaryResponse.java
package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record WorkoutSessionSummaryResponse(
        Long sessionId,
        
        // Información de la sesión
        String sessionTitle,
        String sessionIcon,
        String muscles,
        
        // Tiempos
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Long durationSeconds,       // duración total en segundos
        String durationFormatted,   // "01:23:45" formato legible
        
        // Progreso
        Integer totalExercises,
        Integer completedExercises,
        Integer totalSeries,
        Integer completedSeries,
        Double progressPercentage,
        
        // XP
        Integer xpEarned,
        Integer totalUserXp,        // XP total del usuario después de sumar
        String userRank,            // Rango actual del usuario
        
        // Comparación con sesión anterior (puede ser null)
        PreviousSessionComparison previousComparison
) {}

