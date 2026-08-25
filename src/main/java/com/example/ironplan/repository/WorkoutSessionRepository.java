// src/main/java/com/example/ironplan/repository/WorkoutSessionRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.model.WorkoutSessionStatus;
import com.example.ironplan.repository.projection.SesionFuerzaScoring;
import com.example.ironplan.rest.dto.RecentWorkoutDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    // Última sesión activa de un usuario (si quieres continuarla)
    Optional<WorkoutSession> findFirstByUser_IdAndStatusOrderByStartedAtDesc(
            Long userId,
            WorkoutSessionStatus status
    );
    long countByUser_IdAndStatus(Long userId, WorkoutSessionStatus status);

    List<WorkoutSession> findTop5ByUser_IdAndStatusOrderByStartedAtDesc(
            Long userId,
            WorkoutSessionStatus status
    );
    // Historial de sesiones de un usuario
    List<WorkoutSession> findByUser_IdOrderByStartedAtDesc(Long userId);
    
    // Buscar la sesión anterior completada del mismo routineDetail (para comparación)
    Optional<WorkoutSession> findFirstByUser_IdAndRoutineDetail_IdAndStatusAndIdNotOrderByCompletedAtDesc(
            Long userId,
            Long routineDetailId,
            WorkoutSessionStatus status,
            Long excludeSessionId
    );

    // Buscar todas las sesiones completadas de una rutina específica para un usuario
    // ACTUALIZADO: Ahora navega a través del bloque para llegar a la rutina
    List<WorkoutSession> findByUser_IdAndRoutineDetail_Block_Routine_IdAndStatus(
            Long userId,
            Long routineId,
            WorkoutSessionStatus status
    );

    long countByUser_IdAndRoutineDetail_IdAndStatus(
            Long userId,
            Long routineDetailId,
            WorkoutSessionStatus status
    );

    List<WorkoutSession> findByUser_IdAndStatusOrderByCompletedAtDesc(Long userId, WorkoutSessionStatus status);

    /** Sesiones de fuerza completadas de todo un roster en una sola consulta (scoring TEAM_POINTS). */
    @Query("""
        SELECT new com.example.ironplan.repository.projection.SesionFuerzaScoring(
            s.user.id, s.startedAt, s.completedAt, s.progressPercentage, s.completedExercises)
        FROM WorkoutSession s
        WHERE s.user.id IN :userIds
          AND s.status = com.example.ironplan.model.WorkoutSessionStatus.COMPLETED
          AND s.completedAt BETWEEN :start AND :end
        """)
    List<SesionFuerzaScoring> findScoringDataForUsers(
            @Param("userIds") Collection<Long> userIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
