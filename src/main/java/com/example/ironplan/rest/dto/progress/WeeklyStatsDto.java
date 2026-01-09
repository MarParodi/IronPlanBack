package com.example.ironplan.rest.dto.progress;

import java.time.LocalDate;
import java.util.List;

/**
 * Estadísticas semanales del usuario
 */
public record WeeklyStatsDto(
        LocalDate weekStart,
        LocalDate weekEnd,
        
        int workoutsCompleted,       // Entrenamientos completados
        int totalSets,               // Series totales
        double totalVolumeKg,        // Volumen total
        int totalMinutes,            // Tiempo total
        
        // Desglose por día
        List<DailyWorkoutDto> dailyBreakdown
) {}
