// src/main/java/com/example/ironplan/service/WorkoutExerciseService.java
package com.example.ironplan.service;

import com.example.ironplan.model.WorkoutExercise;
import com.example.ironplan.model.WorkoutExerciseStatus;
import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.repository.WorkoutExerciseRepository;
import com.example.ironplan.repository.WorkoutSessionRepository;
import com.example.ironplan.rest.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WorkoutExerciseService {

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutExerciseRepository workoutExerciseRepo;

    public WorkoutExerciseService(
            WorkoutSessionRepository sessionRepo,
            WorkoutExerciseRepository workoutExerciseRepo
    ) {
        this.sessionRepo = sessionRepo;
        this.workoutExerciseRepo = workoutExerciseRepo;
    }

    // ---------- HELPERS PRIVADOS ----------

    private WorkoutSession getSessionForUser(Long sessionId, Long userId) {
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesión de entrenamiento no encontrada: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            // Podrías crear una excepción de acceso; por simplicidad usamos NotFound
            throw new NotFoundException("Sesión no encontrada para este usuario.");
        }
        return session;
    }

    private WorkoutExercise getExerciseForSession(Long sessionId, Integer exerciseOrder) {
        return workoutExerciseRepo
                .findByWorkoutSession_IdAndExerciseOrder(sessionId, exerciseOrder)
                .orElseThrow(() -> new NotFoundException(
                        "Ejercicio con orden " + exerciseOrder + " no encontrado en la sesión " + sessionId
                ));
    }

    private WorkoutExercise getExerciseByIdForSession(Long sessionId, Long exerciseId) {
        var exercise = workoutExerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Ejercicio de entrenamiento no encontrado: " + exerciseId));

        if (!exercise.getWorkoutSession().getId().equals(sessionId)) {
            throw new NotFoundException("El ejercicio no pertenece a la sesión indicada.");
        }
        return exercise;
    }

    // ---------- OPERACIONES PÚBLICAS ----------

    /**
     * Obtiene un ejercicio por su orden dentro de una sesión,
     * validando que la sesión pertenezca al usuario.
     */
    @Transactional(readOnly = true)
    public WorkoutExercise getExerciseForUserByOrder(Long sessionId, Long userId, Integer exerciseOrder) {
        var session = getSessionForUser(sessionId, userId);
        return getExerciseForSession(session.getId(), exerciseOrder);
    }

    /**
     * Obtiene el siguiente ejercicio PENDING dentro de la sesión.
     * Si no hay ninguno, lanza NotFoundException (o podrías devolver null / Optional).
     */
    @Transactional(readOnly = true)
    public WorkoutExercise getNextPendingExercise(Long sessionId, Long userId) {
        var session = getSessionForUser(sessionId, userId);

        return workoutExerciseRepo
                .findFirstByWorkoutSession_IdAndStatusOrderByExerciseOrderAsc(
                        session.getId(),
                        WorkoutExerciseStatus.PENDING
                )
                .orElseThrow(() -> new NotFoundException(
                        "No hay ejercicios pendientes en esta sesión."
                ));
    }

    /**
     * Marca un ejercicio como COMPLETED (si aún no lo está),
     * actualiza el número de ejercicios completados y el porcentaje de progreso de la sesión.
     */
    @Transactional
    public void markExerciseCompleted(Long sessionId, Long exerciseId, Long userId) {
        var session = getSessionForUser(sessionId, userId);
        var exercise = getExerciseByIdForSession(sessionId, exerciseId);

        // Si ya estaba completado, no duplicamos
        if (exercise.getStatus() != WorkoutExerciseStatus.COMPLETED) {
            exercise.setStatus(WorkoutExerciseStatus.COMPLETED);
            exercise.setCompletedSets(exercise.getPlannedSets());
            exercise.setFinishedAt(LocalDateTime.now());

            // Actualizar progreso de la sesión
            int completed = session.getCompletedExercises() + 1;
            session.setCompletedExercises(completed);

            int total = session.getTotalExercises() != null ? session.getTotalExercises() : 0;
            double progress = 0.0;
            if (total > 0) {
                progress = (completed * 100.0) / total;
            }
            session.setProgressPercentage(progress);

            // XP sencillo: proporcional al progreso vs XP estimada de la RoutineDetail
            if (session.getRoutineDetail() != null && session.getRoutineDetail().getEstimatedXp() != null) {
                int estimatedXp = session.getRoutineDetail().getEstimatedXp();
                int xp = (int) Math.round(estimatedXp * (progress / 100.0));
                session.setXpEarned(xp);
            }

            // Si ya completó todos los ejercicios, marcamos la sesión como COMPLETED
            if (total > 0 && completed >= total) {
                session.setStatus(com.example.ironplan.model.WorkoutSessionStatus.COMPLETED);
                session.setCompletedAt(LocalDateTime.now());
            }

            // Persistimos cambios
            workoutExerciseRepo.save(exercise);
            sessionRepo.save(session);
        }
    }
}
