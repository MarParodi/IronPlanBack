// src/main/java/com/example/ironplan/service/dto/WorkoutSetInput.java
package com.example.ironplan.rest.dto;

public record WorkoutSetInput(
        Integer setNumber,
        Integer reps,
        Double weightKg,
        boolean completed
) {}
