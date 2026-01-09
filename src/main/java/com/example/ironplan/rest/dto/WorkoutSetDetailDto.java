package com.example.ironplan.rest.dto;

public record WorkoutSetDetailDto(
        Long id,
        Integer setNumber,
        Integer reps,
        Double weightKg,
        boolean completed,
        String notes
) {}
