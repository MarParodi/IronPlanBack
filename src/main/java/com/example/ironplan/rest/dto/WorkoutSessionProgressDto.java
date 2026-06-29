// WorkoutSessionProgressDto.java
package com.example.ironplan.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record WorkoutSessionProgressDto(
        Long sessionId,
        int currentExerciseOrder,
        int totalExercises,
        double progressPercentage,
        int xpEarned,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startedAt
) {}
