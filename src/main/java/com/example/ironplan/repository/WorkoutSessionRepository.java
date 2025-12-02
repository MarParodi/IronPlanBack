// src/main/java/com/example/ironplan/repository/WorkoutSessionRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.model.WorkoutSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
