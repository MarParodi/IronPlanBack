package com.example.ironplan.rest.dto.progress;

/**
 * Detalle de una serie individual
 */
public record SetDetailDto(
        int setNumber,
        Integer reps,
        Double weightKg,
        boolean completed
) {}
