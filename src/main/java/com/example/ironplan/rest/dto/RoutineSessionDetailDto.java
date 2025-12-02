// src/main/java/com/example/ironplan/rest/dto/RoutineSessionDetailDto.java
package com.example.ironplan.rest.dto;

import java.util.List;

public record RoutineSessionDetailDto(
        Long id,                 // id de la sesión (RoutineDetail)
        Long routineId,          // id de la rutina (RoutineTemplate)
        String title,            // "TIRÓN"
        String icon,
        String muscles,          // "Glúteo, Espalda, Bíceps"
        String description,      // texto descriptivo largo

        Integer totalSeries,     // 20 (para el encabezado y resumen inferior)
        Integer estimatedMinutes,// 50
        Integer estimatedXp,     // 150

        // Lista de tarjetas (ejercicios de la sesión)
        List<RoutineSessionExerciseDto> exercises
) {}
