// RoutineBlockDto.java
package com.example.ironplan.rest.dto.routine;

import java.util.List;

public record RoutineBlockDto(
        int blockNumber,
        String blockTitle,
        List<RoutineBlockItemDto> sessions
) {}
