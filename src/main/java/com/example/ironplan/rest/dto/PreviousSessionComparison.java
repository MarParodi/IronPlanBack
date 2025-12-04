// PreviousSessionComparison.java
package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record PreviousSessionComparison(
        Long previousSessionId,
        LocalDateTime previousDate,
        Long previousDurationSeconds,
        Integer previousXpEarned,
        
        // Diferencias (positivo = mejora, negativo = peor)
        Long durationDifferenceSeconds,  // diferencia en tiempo
        Integer xpDifference             // diferencia en XP
) {}

