// src/main/java/com/example/ironplan/repository/WorkoutSetRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {

    // Todas las series de un ejercicio, ordenadas
    List<WorkoutSet> findByWorkoutExercise_IdOrderBySetNumberAsc(Long workoutExerciseId);

    // Para mostrar "serie anterior" (la última completada)
    Optional<WorkoutSet> findFirstByWorkoutExercise_IdAndCompletedIsTrueOrderBySetNumberDesc(
            Long workoutExerciseId
    );

    // Si algún día quieres resetear un ejercicio
    void deleteByWorkoutExercise_Id(Long workoutExerciseId);
}

