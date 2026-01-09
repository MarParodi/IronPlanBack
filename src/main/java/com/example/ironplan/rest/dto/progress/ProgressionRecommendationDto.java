package com.example.ironplan.rest.dto.progress;

import java.util.List;

/**
 * Recomendación de progresión para un ejercicio
 */
public record ProgressionRecommendationDto(
        Long exerciseId,
        String exerciseName,
        
        // Configuración del ejercicio (de RoutineExercise)
        int plannedSets,
        int repsMin,
        int repsMax,
        
        // Datos de los últimos 3 entrenamientos
        List<RecentPerformanceDto> recentPerformance,
        
        // La recomendación
        RecommendationType type,
        String message,
        
        // Valores sugeridos
        Double suggestedWeightKg,
        Integer suggestedRepsTarget
) {
    public enum RecommendationType {
        INCREASE_WEIGHT,    // Subir peso
        DECREASE_WEIGHT,    // Bajar peso  
        INCREASE_REPS,      // Mantener peso, subir reps
        MAINTAIN,           // Mantener igual
        FIRST_TIME          // Primera vez haciendo el ejercicio
    }
}
