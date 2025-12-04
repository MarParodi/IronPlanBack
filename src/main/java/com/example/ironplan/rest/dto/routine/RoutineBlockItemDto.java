// RoutineBlockItemDto.java
package com.example.ironplan.rest.dto.routine;

public record RoutineBlockItemDto(
        Long sessionId,
        String title,
        int totalSeries,
        String mainMuscles
) {}
