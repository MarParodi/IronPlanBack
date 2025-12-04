// ProfileHeaderDto.java
package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record ProfileHeaderDto(
        Long userId,
        String username,
        String email,
        String trainingLevel,   // tu enum Level.toString()
        int xpPoints,
        int lifetimeXp,
        String xpRankCode,      // NOVATO_II
        String xpRankLabel,     // "Novato II"
        LocalDateTime joinedAt
) {}
