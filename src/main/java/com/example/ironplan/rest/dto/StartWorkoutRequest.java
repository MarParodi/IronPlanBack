// StartWorkoutRequest.java
package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.NotNull;

public record StartWorkoutRequest(
        @NotNull
        Long routineDetailId
) {}
