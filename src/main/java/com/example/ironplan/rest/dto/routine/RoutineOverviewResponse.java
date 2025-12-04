// RoutineOverviewResponse.java
package com.example.ironplan.rest.dto.routine;

import java.util.List;

public record RoutineOverviewResponse(
        Long id,
        String name,
        Integer durationWeeks,
        String longDescription,
        String goal,            // "Hipertrofia"
        String recommendedLevel,// "Intermedio"
        Integer daysPerWeek,    // 3
        List<RoutineBlockDto> blocks
) {}
