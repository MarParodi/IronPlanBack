// src/main/java/com/example/ironplan/rest/dto/RoutineSessionExerciseDto.java
package com.example.ironplan.rest.dto;

public record RoutineSessionExerciseDto(
        Long id,          // id del RoutineExercise
        Long exerciseId,  // id del Exercise real (por si luego quieres ver más detalles)
        String name,      // "Peso muerto rumano"
        Integer order,    // orden dentro de la sesión (1, 2, 3...)
        Integer sets,     // 3
        Integer repsMin,  // 7
        Integer repsMax,  // 9
        Integer rir,      // 1
        Integer restMinutes,// opcional, ej. 90 (para futuro)
        String instructions
) {}
