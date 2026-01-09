package com.example.ironplan.rest.dto.progress;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Resumen general de progreso del usuario
 */
public record ProgressSummaryDto(
        // Totales acumulados
        int totalWorkouts,
        int totalSets,
        double totalVolumeKg,
        int totalMinutes,
        
        // Frecuencia y consistencia
        double avgWorkoutsPerWeek,   // Promedio entrenamientos por semana
        int currentStreak,           // Días consecutivos entrenando (o semanas)
        int longestStreak,           // Racha más larga
        
        // PRs globales (opcional: top 3 ejercicios por volumen)
        List<ExercisePrDto> topExercises,
        
        // Últimas 4-8 semanas para gráfico
        List<WeeklyStatsDto> weeklyHistory
) {}
