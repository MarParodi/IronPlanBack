// src/main/java/com/example/ironplan/service/WorkoutSetService.java
package com.example.ironplan.service;

import com.example.ironplan.model.WorkoutExercise;
import com.example.ironplan.model.WorkoutExerciseStatus;
import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.model.WorkoutSessionStatus;
import com.example.ironplan.model.WorkoutSet;
import com.example.ironplan.model.XpEventType;
import com.example.ironplan.repository.WorkoutExerciseRepository;
import com.example.ironplan.repository.WorkoutSessionRepository;
import com.example.ironplan.repository.WorkoutSetRepository;
import com.example.ironplan.rest.error.NotFoundException;
import com.example.ironplan.rest.dto.WorkoutSetInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class WorkoutSetService {

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutExerciseRepository workoutExerciseRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final XpService xpService;

    public WorkoutSetService(
            WorkoutSessionRepository sessionRepo,
            WorkoutExerciseRepository workoutExerciseRepo,
            WorkoutSetRepository workoutSetRepo,
            XpService xpService
    ) {
        this.sessionRepo = sessionRepo;
        this.workoutExerciseRepo = workoutExerciseRepo;
        this.workoutSetRepo = workoutSetRepo;
        this.xpService = xpService;
    }

    // ---------- HELPERS PRIVADOS ----------

    private WorkoutSession getSessionForUser(Long sessionId, Long userId) {
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesión de entrenamiento no encontrada: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new NotFoundException("Sesión no encontrada para este usuario.");
        }
        return session;
    }

    private WorkoutExercise getExerciseForSession(Long sessionId, Long exerciseId) {
        var exercise = workoutExerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Ejercicio de entrenamiento no encontrado: " + exerciseId));

        if (!exercise.getWorkoutSession().getId().equals(sessionId)) {
            throw new NotFoundException("El ejercicio no pertenece a la sesión indicada.");
        }
        return exercise;
    }

    // ---------- MÉTODO PRINCIPAL ----------

    /**
     * Guarda o actualiza las series (sets) de un ejercicio dentro de una sesión.
     *
     * - Valida que la sesión pertenezca al usuario.
     * - Valida que el ejercicio pertenezca a la sesión.
     * - Crea/actualiza WorkoutSet para cada setNumber enviado.
     * - Actualiza completedSets y status del WorkoutExercise.
     * - Actualiza progreso y XP de la WorkoutSession.
     */
    @Transactional
    public void saveSetsForExercise(
            Long sessionId,
            Long exerciseId,
            Long userId,
            List<WorkoutSetInput> setsInput,
            String exerciseNotes // por si luego quieres guardar notas generales del ejercicio
    ) {
        var session = getSessionForUser(sessionId, userId);
        var exercise = getExerciseForSession(sessionId, exerciseId);

        // Marcamos el ejercicio como iniciado si es la primera vez
        if (exercise.getStatus() == WorkoutExerciseStatus.PENDING) {
            exercise.setStatus(WorkoutExerciseStatus.IN_PROGRESS);
            exercise.setStartedAt(LocalDateTime.now());
        }

        // Traemos las series actuales
        var existingSets = workoutSetRepo
                .findByWorkoutExercise_IdOrderBySetNumberAsc(exercise.getId());

        // Mapeamos por número de serie para fácil acceso
        var existingByNumber = existingSets.stream()
                .collect(java.util.stream.Collectors.toMap(
                        WorkoutSet::getSetNumber,
                        s -> s
                ));

        // Procesamos cada input
        for (WorkoutSetInput input : setsInput) {
            if (input.setNumber() == null || input.setNumber() <= 0) {
                continue; // ignoramos entradas inválidas
            }

            var set = existingByNumber.get(input.setNumber());
            if (set == null) {
                set = new WorkoutSet();
                set.setWorkoutExercise(exercise);
                set.setSetNumber(input.setNumber());
            }

            set.setReps(input.reps());
            set.setWeightKg(input.weightKg());
            set.setCompleted(input.completed());

            // (opcional) aquí podrías manejar notes por set si quisieras
            // set.setNotes(...);

            workoutSetRepo.save(set);
        }

        // Recalcular estado del ejercicio: cuántas series completadas
        var updatedSets = workoutSetRepo
                .findByWorkoutExercise_IdOrderBySetNumberAsc(exercise.getId());

        long completedCount = updatedSets.stream()
                .filter(WorkoutSet::isCompleted)
                .count();

        exercise.setCompletedSets((int) completedCount);

        // Si ya completó todas las series planeadas → marcar ejercicio COMPLETED
        int plannedSets = exercise.getPlannedSets() != null ? exercise.getPlannedSets() : 0;
        if (plannedSets > 0 && completedCount >= plannedSets) {
            exercise.setStatus(WorkoutExerciseStatus.COMPLETED);
            exercise.setFinishedAt(LocalDateTime.now());
        }

        workoutExerciseRepo.save(exercise);

        // Recalcular progreso de la sesión y XP
        recalculateSessionProgressAndXp(session);
    }

    // ---------- RECALCULAR PROGRESO / XP ----------

    private void recalculateSessionProgressAndXp(WorkoutSession session) {
        // Traemos todos los ejercicios de la sesión
        var exercises = workoutExerciseRepo
                .findByWorkoutSession_IdOrderByExerciseOrderAsc(session.getId());

        int total = session.getTotalExercises() != null ? session.getTotalExercises() : exercises.size();
        int completed = (int) exercises.stream()
                .filter(e -> e.getStatus() == WorkoutExerciseStatus.COMPLETED)
                .count();

        session.setCompletedExercises(completed);

        double progress = 0.0;
        if (total > 0) {
            progress = (completed * 100.0) / total;
        }
        session.setProgressPercentage(progress);

        // XP proporcional usando estimatedXp de RoutineDetail
        int xpToGrant = 0;
        if (session.getRoutineDetail() != null && session.getRoutineDetail().getEstimatedXp() != null) {
            int estimatedXp = session.getRoutineDetail().getEstimatedXp();
            xpToGrant = (int) Math.round(estimatedXp * (progress / 100.0));
            session.setXpEarned(xpToGrant);
        }

        // Si ya completó todo, marcamos la sesión como COMPLETED y sumamos XP al usuario
        boolean wasActive = session.getStatus() == WorkoutSessionStatus.ACTIVE;
        if (total > 0 && completed >= total && wasActive) {
            session.setStatus(WorkoutSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            
            // ✅ SUMAR XP AL USUARIO
            if (xpToGrant > 0) {
                String description = String.format(
                    "Sesión completada: %s",
                    session.getRoutineDetail() != null ? session.getRoutineDetail().getTitle() : "Entrenamiento"
                );
                xpService.grantXp(session.getUser(), xpToGrant, XpEventType.WORKOUT_COMPLETED, description);
            }
        }

        sessionRepo.save(session);
    }
}
