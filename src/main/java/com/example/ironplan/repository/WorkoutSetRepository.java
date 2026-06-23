// src/main/java/com/example/ironplan/repository/WorkoutSetRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
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
    
    // Para borrar todos los sets de un ejercicio
    void deleteAllByWorkoutExercise_Id(Long workoutExerciseId);
    
    // Contar sets completados de un ejercicio
    long countByWorkoutExercise_IdAndCompletedTrue(Long workoutExerciseId);


    @Query("""
    SELECT ws
    FROM WorkoutSet ws
    JOIN ws.workoutExercise we
    JOIN we.workoutSession s
    JOIN we.routineExercise re
    JOIN re.exercise ex
    WHERE s.user.id = :userId
      AND ex.id = :exerciseId
      AND ws.completed = true
    ORDER BY ws.createdAt DESC
""")
    List<WorkoutSet> findLastCompletedSetForUserAndExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId,
            Pageable pageable
    );

    @Query("""
        SELECT MAX(ws.oneRmEstimado) FROM WorkoutSet ws
        JOIN ws.workoutExercise we
        JOIN we.workoutSession s
        WHERE s.user.id = :userId
          AND s.status = com.example.ironplan.model.WorkoutSessionStatus.COMPLETED
          AND ws.completed = true
          AND ws.oneRmEstimado IS NOT NULL
          AND CAST(s.completedAt AS localdate) >= :startDate
          AND CAST(s.completedAt AS localdate) <= :endDate
        """)
    java.math.BigDecimal maxOneRmInPeriod(
            @Param("userId") Long userId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    @Query("""
        SELECT ws.oneRmEstimado FROM WorkoutSet ws
        JOIN ws.workoutExercise we
        JOIN we.workoutSession s
        WHERE s.user.id = :userId
          AND s.status = com.example.ironplan.model.WorkoutSessionStatus.COMPLETED
          AND ws.completed = true
          AND ws.oneRmEstimado IS NOT NULL
          AND CAST(s.completedAt AS localdate) >= :startDate
          AND CAST(s.completedAt AS localdate) <= :endDate
        ORDER BY s.completedAt ASC
        """)
    List<java.math.BigDecimal> findOneRmValuesChronological(
            @Param("userId") Long userId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );
}

