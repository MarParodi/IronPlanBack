package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActiveRoutineResponse(
        Long id,
        String name,
        Integer durationWeeks,
        Integer daysPerWeek,
        Integer totalSessions,
        Integer completedSessions,
        Integer progressPercent,
        LocalDateTime startedAt,
        List<ActiveRoutineBlockDto> blocks
) {}

