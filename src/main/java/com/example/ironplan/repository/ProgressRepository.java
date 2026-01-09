package com.example.ironplan.repository;

import com.example.ironplan.model.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio especializado para queries de progreso y estadísticas.
 * Todas las queries están optimizadas para evitar N+1.
 */
public interface ProgressRepository extends JpaRepository<WorkoutSet, Long> {

    // ============ VOLUMEN POR EJERCICIO ============
    
    /**
     * Obtiene todas las series completadas de un ejercicio específico para un usuario,
     * ordenadas por fecha descendente. Incluye info del ejercicio y sesión.
     * Usa JOIN FETCH para evitar N+1.
     */
    @Query("""
        SELECT ws FROM WorkoutSet ws
        JOIN FETCH ws.workoutExercise we
        JOIN FETCH we.workoutSession s
        JOIN FETCH we.routineExercise re
        JOIN FETCH re.exercise ex
        WHERE s.user.id = :userId
          AND ex.id = :exerciseId
          AND ws.completed = true
          AND s.status = 'COMPLETED'
        ORDER BY s.completedAt DESC, ws.setNumber ASC
    """)
    List<WorkoutSet> findAllCompletedSetsForExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId
    );

    /**
     * Obtiene los últimos N workout exercises de un ejercicio específico (para recomendación)
     */
    @Query("""
        SELECT DISTINCT we FROM WorkoutExercise we
        JOIN FETCH we.workoutSession s
        JOIN FETCH we.routineExercise re
        JOIN FETCH re.exercise ex
        WHERE s.user.id = :userId
          AND ex.id = :exerciseId
          AND s.status = 'COMPLETED'
        ORDER BY s.completedAt DESC
    """)
    List<com.example.ironplan.model.WorkoutExercise> findRecentWorkoutExercises(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId
    );

    // ============ VOLUMEN POR SEMANA ============
    
    /**
     * Obtiene todas las series completadas en un rango de fechas.
     * Para calcular volumen semanal.
     */
    @Query("""
        SELECT ws FROM WorkoutSet ws
        JOIN FETCH ws.workoutExercise we
        JOIN FETCH we.workoutSession s
        WHERE s.user.id = :userId
          AND ws.completed = true
          AND s.status = 'COMPLETED'
          AND s.completedAt >= :startDate
          AND s.completedAt < :endDate
        ORDER BY s.completedAt ASC
    """)
    List<WorkoutSet> findCompletedSetsInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ============ TOP SET GLOBAL ============
    
    /**
     * Obtiene el top set (mayor peso) de un ejercicio para un usuario
     */
    @Query("""
        SELECT ws FROM WorkoutSet ws
        JOIN ws.workoutExercise we
        JOIN we.workoutSession s
        JOIN we.routineExercise re
        JOIN re.exercise ex
        WHERE s.user.id = :userId
          AND ex.id = :exerciseId
          AND ws.completed = true
          AND ws.weightKg IS NOT NULL
          AND s.status = 'COMPLETED'
        ORDER BY ws.weightKg DESC, ws.reps DESC
    """)
    List<WorkoutSet> findTopSetForExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId
    );

    // ============ CONTEO Y TOTALES ============
    
    /**
     * Cuenta entrenamientos completados por usuario
     */
    @Query("""
        SELECT COUNT(DISTINCT s) FROM WorkoutSession s
        WHERE s.user.id = :userId
          AND s.status = 'COMPLETED'
    """)
    long countCompletedWorkouts(@Param("userId") Long userId);

    /**
     * Cuenta series completadas por usuario
     */
    @Query("""
        SELECT COUNT(ws) FROM WorkoutSet ws
        JOIN ws.workoutExercise we
        JOIN we.workoutSession s
        WHERE s.user.id = :userId
          AND ws.completed = true
          AND s.status = 'COMPLETED'
    """)
    long countCompletedSets(@Param("userId") Long userId);

    /**
     * Suma volumen total (peso * reps) por usuario
     */
    @Query("""
        SELECT COALESCE(SUM(ws.weightKg * ws.reps), 0) FROM WorkoutSet ws
        JOIN ws.workoutExercise we
        JOIN we.workoutSession s
        WHERE s.user.id = :userId
          AND ws.completed = true
          AND ws.weightKg IS NOT NULL
          AND ws.reps IS NOT NULL
          AND s.status = 'COMPLETED'
    """)
    Double sumTotalVolume(@Param("userId") Long userId);

    /**
     * Suma tiempo total de entrenamiento (minutos)
     */
    @Query("""
        SELECT COALESCE(SUM(TIMESTAMPDIFF(MINUTE, s.startedAt, s.completedAt)), 0)
        FROM WorkoutSession s
        WHERE s.user.id = :userId
          AND s.status = 'COMPLETED'
          AND s.startedAt IS NOT NULL
          AND s.completedAt IS NOT NULL
    """)
    Long sumTotalMinutes(@Param("userId") Long userId);

    // ============ FRECUENCIA Y STREAK ============
    
    /**
     * Obtiene las fechas de entrenamientos completados (para calcular streak)
     */
    @Query("""
        SELECT DISTINCT CAST(s.completedAt AS LocalDate)
        FROM WorkoutSession s
        WHERE s.user.id = :userId
          AND s.status = 'COMPLETED'
          AND s.completedAt IS NOT NULL
        ORDER BY CAST(s.completedAt AS LocalDate) DESC
    """)
    List<java.time.LocalDate> findWorkoutDates(@Param("userId") Long userId);

    /**
     * Cuenta entrenamientos por semana en las últimas N semanas
     */
    @Query("""
        SELECT CAST(s.completedAt AS LocalDate), COUNT(s)
        FROM WorkoutSession s
        WHERE s.user.id = :userId
          AND s.status = 'COMPLETED'
          AND s.completedAt >= :startDate
        GROUP BY CAST(s.completedAt AS LocalDate)
        ORDER BY CAST(s.completedAt AS LocalDate) DESC
    """)
    List<Object[]> countWorkoutsByDate(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    // ============ TOP EJERCICIOS POR VOLUMEN ============
    
    /**
     * Obtiene los ejercicios con más volumen total
     */
    @Query("""
        SELECT ex.id, ex.name, ex.primaryMuscle,
               MAX(ws.weightKg), 
               SUM(ws.weightKg * ws.reps)
        FROM WorkoutSet ws
        JOIN ws.workoutExercise we
        JOIN we.workoutSession s
        JOIN we.routineExercise re
        JOIN re.exercise ex
        WHERE s.user.id = :userId
          AND ws.completed = true
          AND ws.weightKg IS NOT NULL
          AND ws.reps IS NOT NULL
          AND s.status = 'COMPLETED'
        GROUP BY ex.id, ex.name, ex.primaryMuscle
        ORDER BY SUM(ws.weightKg * ws.reps) DESC
    """)
    List<Object[]> findTopExercisesByVolume(@Param("userId") Long userId);
}
