// WorkoutPreviousSetDto.java
package com.example.ironplan.rest.dto;

public record WorkoutPreviousSetDto(
        Integer setNumber,
        Integer reps,
        Double weightKg
) {}
