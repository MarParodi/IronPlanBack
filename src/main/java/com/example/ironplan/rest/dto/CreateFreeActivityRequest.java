package com.example.ironplan.rest.dto;

import com.example.ironplan.model.FreeActivityType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateFreeActivityRequest(
        @NotNull FreeActivityType activityType,
        String activityTypeOther,
        @Min(0) Double distanceKm,
        @NotNull @Min(1) Integer durationSeconds,
        String photoUrl,
        String notes,
        Integer caloriesEstimated
) {}
