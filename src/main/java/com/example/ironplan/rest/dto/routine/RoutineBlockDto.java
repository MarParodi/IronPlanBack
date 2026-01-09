// RoutineBlockDto.java
package com.example.ironplan.rest.dto.routine;

import java.util.List;

public record RoutineBlockDto(
        Long id,
        int orderIndex,
        String name,
        String description,
        int durationWeeks,
        List<RoutineBlockItemDto> sessions
) {}
