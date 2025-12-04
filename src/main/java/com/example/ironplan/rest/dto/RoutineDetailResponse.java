package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Access_Type;
import com.example.ironplan.model.Goal;
import com.example.ironplan.model.Level;

public record RoutineDetailResponse(
        Long id,
        String name,
        Goal goal,
        Access_Type access,
        String description,
        Level suggestedLevel
) {}
