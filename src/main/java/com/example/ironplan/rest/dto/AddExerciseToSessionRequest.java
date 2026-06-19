package com.example.ironplan.rest.dto;

public record AddExerciseToSessionRequest(
        Long exerciseId,
        Integer plannedSets,
        Integer plannedRepsMin,
        Integer plannedRepsMax
) {}
