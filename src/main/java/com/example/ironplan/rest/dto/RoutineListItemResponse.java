// src/main/java/com/example/ironplan/rest/dto/RoutineListItemResponse.java
package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Access_Type;
import com.example.ironplan.model.Goal;
import com.example.ironplan.model.RoutineGender;

public record RoutineListItemResponse(
        Long id,
        String name,          // = title
        Goal goal,
        Access_Type accessType,
        String img,           // = thumbnailUrl
        String description,
        String ownerUsername, // Username del creador (cuando accessType es USER_SHARED)
        Integer usageCount,
        RoutineGender routineGender,
        Integer xpCost,       // Costo en XP para desbloquear
        Boolean unlockedByUser // true si el usuario actual ya desbloqueó esta rutina
) {}
