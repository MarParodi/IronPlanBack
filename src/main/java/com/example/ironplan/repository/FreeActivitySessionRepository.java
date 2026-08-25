package com.example.ironplan.repository;

import com.example.ironplan.model.FreeActivitySession;
import com.example.ironplan.repository.projection.ActividadLibreScoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FreeActivitySessionRepository extends JpaRepository<FreeActivitySession, Long> {

    List<FreeActivitySession> findByUser_IdOrderByCompletedAtDesc(Long userId);

    List<FreeActivitySession> findByUser_IdAndCompletedAtBetweenOrderByCompletedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    /** Actividades libres de todo un roster en una sola consulta (scoring TEAM_POINTS). */
    @Query("""
        SELECT new com.example.ironplan.repository.projection.ActividadLibreScoring(
            s.user.id, s.completedAt, s.activityType, s.durationSeconds, s.photoUrl)
        FROM FreeActivitySession s
        WHERE s.user.id IN :userIds
          AND s.completedAt BETWEEN :start AND :end
        """)
    List<ActividadLibreScoring> findScoringDataForUsers(
            @Param("userIds") Collection<Long> userIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
