// src/main/java/com/example/ironplan/rest/dto/RoutineListItemResponse.java
package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Access_Type;
import com.example.ironplan.model.Goal;

public record RoutineListItemResponse(
        Long id,
        String name,          // = title
        Goal goal,
        Access_Type accessType,
        String img,           // = thumbnailUrl
        String description
) {}
