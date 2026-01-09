package com.example.ironplan.rest.dto.progress;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Historial de progreso de un ejercicio específico
 */
public record ExerciseProgressDto(
        Long exerciseId,
        String exerciseName,
        String primaryMuscle,
        
        // Estadísticas generales
        int totalSessions,           // Veces que se ha hecho este ejercicio
        double totalVolumeKg,        // Volumen total acumulado (peso * reps)
        
        // Personal Records (PRs)
        TopSetDto topSet,            // Mejor set por peso
        Double estimated1RM,         // 1RM estimado (Epley)
        
        // Historial por sesión (últimas N)
        List<ExerciseSessionHistoryDto> history
) {}
