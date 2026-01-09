package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Access_Type;
import com.example.ironplan.model.Goal;
import com.example.ironplan.model.Level;

public record RoutineDetailResponse(
        Long id,
        String name,
        Goal goal,
        Access_Type access,
        String img,
        String description,
        Level suggestedLevel,
        Integer xp_cost
) {}
