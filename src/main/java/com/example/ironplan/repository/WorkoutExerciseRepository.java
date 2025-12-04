// src/main/java/com/example/ironplan/repository/WorkoutExerciseRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.WorkoutExercise;
import com.example.ironplan.model.WorkoutExerciseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {

    // Todos los ejercicios de una sesión, en orden
    List<WorkoutExercise> findByWorkoutSession_IdOrderByExerciseOrderAsc(Long workoutSessionId);

    // Buscar un ejercicio específico por sesión + orden (ej: 2 de 7)
    Optional<WorkoutExercise> findByWorkoutSession_IdAndExerciseOrder(
            Long workoutSessionId,
            Integer exerciseOrder
    );

    // Siguiente ejercicio pendiente dentro de la sesión
    Optional<WorkoutExercise> findFirstByWorkoutSession_IdAndStatusOrderByExerciseOrderAsc(
            Long workoutSessionId,
            WorkoutExerciseStatus status
    );
}
