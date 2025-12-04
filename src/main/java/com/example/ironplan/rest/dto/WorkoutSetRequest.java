package com.example.ironplan.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record WorkoutSetRequest(
        @NotEmpty
        @Valid
        List<WorkoutSetItemRequest> sets,

        String notes // notas generales del ejercicio (opcional)
) {}
