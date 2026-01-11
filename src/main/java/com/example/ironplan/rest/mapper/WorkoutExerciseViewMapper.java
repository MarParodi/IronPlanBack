package com.example.ironplan.rest.mapper;

import com.example.ironplan.model.Exercise;
import com.example.ironplan.model.RoutineExercise;
import com.example.ironplan.model.WorkoutExercise;
import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.model.WorkoutSet;
import com.example.ironplan.rest.dto.NextExerciseSummaryDto;
import com.example.ironplan.rest.dto.WorkoutExerciseDetailResponse;
import com.example.ironplan.rest.dto.WorkoutPreviousSetDto;
import com.example.ironplan.rest.dto.WorkoutSessionProgressDto;

import java.util.Comparator;
import java.util.Collections;
import java.util.List;

public final class WorkoutExerciseViewMapper {

    private WorkoutExerciseViewMapper() {}

    public static WorkoutExerciseDetailResponse toDetailResponse(
            WorkoutSession session,
            WorkoutExercise exercise,
            WorkoutSet previousSet
    ) {
        // 1) Serie anterior
        WorkoutPreviousSetDto previousDto = null;
        if (previousSet != null) {
            previousDto = new WorkoutPreviousSetDto(
                    previousSet.getSetNumber(),
                    previousSet.getReps(),
                    previousSet.getWeightKg()
            );
        }

        // 2) Lista de ejercicios de la sesión (puede venir null)
        List<WorkoutExercise> allExercises = session.getWorkoutExercises();
        if (allExercises == null) {
            allExercises = Collections.emptyList();
        }

        // 3) Total de ejercicios: primero usamos el campo de la sesión,
        //    si viene null, caemos al tamaño de la lista (o 0 si también está vacía)
        int totalExercises = session.getTotalExercises() != null
                ? session.getTotalExercises()
                : allExercises.size();

        // 4) Progreso de la sesión
        WorkoutSessionProgressDto progressDto = new WorkoutSessionProgressDto(
                session.getId(),
                exercise.getExerciseOrder(),
                totalExercises,
                session.getProgressPercentage() != null ? session.getProgressPercentage() : 0.0,
                session.getXpEarned() != null ? session.getXpEarned() : 0,
                session.getStartedAt() != null ? session.getStartedAt() : session.getCreatedAt()
        );

        // 5) Lista de siguientes ejercicios (si no hay lista, queda vacío)
        List<NextExerciseSummaryDto> nextDtos = allExercises.stream()
                .filter(we -> we.getExerciseOrder() > exercise.getExerciseOrder())
                .sorted(Comparator.comparingInt(WorkoutExercise::getExerciseOrder))
                .map(next -> {
                    // Soporte para sesiones de rutina Y personalizadas
                    Exercise base = resolveCatalogExercise(next);

                    return new NextExerciseSummaryDto(
                            next.getId(),
                            next.getExerciseOrder(),
                            next.getExerciseName(),
                            next.getPlannedSets(),
                            next.getPlannedRepsMin(),
                            next.getPlannedRepsMax(),
                            next.getPlannedRir(),
                            base != null ? base.getId() : null,
                            base != null ? base.getVideoUrl() : null
                    );
                })
                .toList();

        // 6) Datos del ejercicio actual (soporte para rutina Y personalizadas)
        Exercise currentBase = resolveCatalogExerciseSafe(exercise);

        return new WorkoutExerciseDetailResponse(
                session.getId(),
                exercise.getId(),
                exercise.getExerciseOrder(),
                exercise.getExerciseName(),
                exercise.getPlannedSets(),
                exercise.getPlannedRepsMin(),
                exercise.getPlannedRepsMax(),
                exercise.getPlannedRir(),
                exercise.getPlannedRestSeconds(),
                currentBase != null ? currentBase.getId() : null,
                currentBase != null ? currentBase.getVideoUrl() : null,
                previousDto,
                progressDto,
                nextDtos
        );
    }

    /**
     * Resuelve el ejercicio de catálogo, lanza excepción si no existe.
     */
    private static Exercise resolveCatalogExercise(WorkoutExercise we) {
        if (we.getRoutineExercise() != null && we.getRoutineExercise().getExercise() != null) {
            return we.getRoutineExercise().getExercise();
        }

        if (we.getExercise() != null) {
            return we.getExercise();
        }

        throw new IllegalStateException(
                "WorkoutExercise sin routineExercise ni exercise asociado (id=" + we.getId() + ")"
        );
    }

    /**
     * Resuelve el ejercicio de catálogo, retorna null si no existe (no lanza excepción).
     */
    private static Exercise resolveCatalogExerciseSafe(WorkoutExercise we) {
        if (we.getRoutineExercise() != null && we.getRoutineExercise().getExercise() != null) {
            return we.getRoutineExercise().getExercise();
        }

        if (we.getExercise() != null) {
            return we.getExercise();
        }

        return null;
    }

}
