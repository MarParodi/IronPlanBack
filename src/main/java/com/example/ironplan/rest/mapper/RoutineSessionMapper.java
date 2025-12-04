// src/main/java/com/example/ironplan/rest/mapper/RoutineSessionMapper.java
package com.example.ironplan.rest.mapper;

import com.example.ironplan.model.RoutineDetail;
import com.example.ironplan.model.RoutineExercise;
import com.example.ironplan.rest.dto.RoutineSessionDetailDto;
import com.example.ironplan.rest.dto.RoutineSessionExerciseDto;

import java.util.Comparator;
import java.util.List;

public final class RoutineSessionMapper {

    private RoutineSessionMapper() { }

    public static RoutineSessionExerciseDto toExerciseDto(RoutineExercise e) {
        if (e == null) return null;
        return new RoutineSessionExerciseDto(
                e.getId(),
                e.getExercise() != null ? e.getExercise().getId() : null,
                e.getDisplayName() != null ? e.getDisplayName()
                        : (e.getExercise() != null ? e.getExercise().getName() : null),
                e.getExerciseOrder(),
                e.getSets(),
                e.getRepsMin(),
                e.getRepsMax(),
                e.getRir(),
                e.getRestMinutes()
        );
    }

    public static RoutineSessionDetailDto toDetailDto(RoutineDetail d) {
        if (d == null) return null;

        // ordenamos ejercicios por exerciseOrder por si acaso
        List<RoutineSessionExerciseDto> exerciseDtos = d.getExercises()
                .stream()
                .sorted(Comparator.comparing(RoutineExercise::getExerciseOrder))
                .map(RoutineSessionMapper::toExerciseDto)
                .toList();

        return new RoutineSessionDetailDto(
                d.getId(),
                d.getRoutine() != null ? d.getRoutine().getId() : null,
                d.getTitle(),
                d.getIcon(),
                d.getMuscles(),
                d.getDescription(),
                d.getTotalSeries(),
                d.getEstimatedMinutes(),
                d.getEstimatedXp(),
                exerciseDtos
        );
    }
}
