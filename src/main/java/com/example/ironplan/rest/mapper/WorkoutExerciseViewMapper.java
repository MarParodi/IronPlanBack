package com.example.ironplan.rest.mapper;

import com.example.ironplan.model.Exercise;
import com.example.ironplan.model.WorkoutExercise;
import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.model.WorkoutSet;
import com.example.ironplan.rest.dto.WorkoutExerciseDetailResponse;
import com.example.ironplan.rest.dto.WorkoutPreviousSetDto;
import com.example.ironplan.rest.dto.WorkoutSessionProgressDto;

public final class WorkoutExerciseViewMapper {

    private WorkoutExerciseViewMapper() {}

    public static WorkoutExerciseDetailResponse toDetailResponse(
            WorkoutSession session,
            WorkoutExercise exercise,
            WorkoutSet previousSet
    ) {
        Exercise catalogExercise = exercise.getRoutineExercise() != null
                ? exercise.getRoutineExercise().getExercise()
                : null;

        WorkoutPreviousSetDto previousDto = null;
        if (previousSet != null) {
            previousDto = new WorkoutPreviousSetDto(
                    previousSet.getSetNumber(),
                    previousSet.getReps(),
                    previousSet.getWeightKg()
            );
        }

        WorkoutSessionProgressDto progressDto = new WorkoutSessionProgressDto(
                session.getId(),
                exercise.getExerciseOrder(),
                session.getTotalExercises() != null ? session.getTotalExercises() : 0,
                session.getProgressPercentage() != null ? session.getProgressPercentage() : 0.0,
                session.getXpEarned() != null ? session.getXpEarned() : 0
        );

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
                catalogExercise != null ? catalogExercise.getId() : null,
                catalogExercise != null ? catalogExercise.getVideoUrl() : null,
                previousDto,
                progressDto
        );
    }
}
