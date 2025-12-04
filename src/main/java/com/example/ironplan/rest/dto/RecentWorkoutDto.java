// RecentWorkoutDto.java
package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record RecentWorkoutDto(
        Long sessionId,
        String routineName,      // nombre de la sesión / rutina
        LocalDateTime date,      // startedAt
        int totalSeries,
        double totalWeightKg,
        long durationMinutes
) {}
