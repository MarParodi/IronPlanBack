package com.example.ironplan.rest.dto.progress;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen de un ejercicio en una sesión específica
 */
public record ExerciseSessionHistoryDto(
        Long workoutExerciseId,
        LocalDateTime date,
        String sessionName,         // "Tirón", "Empuje", etc.
        
        // Métricas de la sesión
        double volumeKg,            // Peso * reps total
        TopSetDto topSet,           // Mejor set de la sesión
        Double estimated1RM,        // 1RM estimado del top set
        int totalSets,
        int completedSets,
        
        // Detalle de cada serie
        List<SetDetailDto> sets
) {}
