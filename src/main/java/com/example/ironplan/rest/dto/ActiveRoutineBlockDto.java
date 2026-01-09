package com.example.ironplan.rest.dto;

import java.util.List;

public record ActiveRoutineBlockDto(
        Long blockId,
        Integer orderIndex,
        String name,
        String description,
        Integer durationWeeks,
        List<ActiveRoutineSessionDto> sessions
) {}
