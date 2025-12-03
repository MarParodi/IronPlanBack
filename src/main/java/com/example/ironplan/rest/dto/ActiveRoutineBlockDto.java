package com.example.ironplan.rest.dto;

import java.util.List;

public record ActiveRoutineBlockDto(
        Integer blockNumber,
        String blockTitle,
        List<ActiveRoutineSessionDto> sessions
) {}

