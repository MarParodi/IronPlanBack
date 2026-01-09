package com.example.ironplan.rest.dto.progress;

import java.time.LocalDate;

/**
 * Resumen de un día de entrenamiento
 */
public record DailyWorkoutDto(
        LocalDate date,
        int dayOfWeek,              // 1=Lun, 7=Dom
        boolean hasWorkout,
        Long workoutSessionId,
        String sessionName,
        int sets,
        double volumeKg,
        int minutes
) {}
