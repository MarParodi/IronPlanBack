package com.example.ironplan.service;

import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.repository.WorkoutExerciseRepository;
import com.example.ironplan.repository.WorkoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Efectos secundarios al completar una sesión (notifs, progresión, hazañas).
 * Corre en otro hilo después del commit para no bloquear el POST de series.
 */
@Service
public class WorkoutSessionCompletedHooks {

    private static final Logger log = LoggerFactory.getLogger(WorkoutSessionCompletedHooks.class);

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutExerciseRepository workoutExerciseRepo;
    private final NotificationService notificationService;
    private final ProgressService progressService;
    private final AchievementService achievementService;

    public WorkoutSessionCompletedHooks(
            WorkoutSessionRepository sessionRepo,
            WorkoutExerciseRepository workoutExerciseRepo,
            NotificationService notificationService,
            ProgressService progressService,
            AchievementService achievementService
    ) {
        this.sessionRepo = sessionRepo;
        this.workoutExerciseRepo = workoutExerciseRepo;
        this.notificationService = notificationService;
        this.progressService = progressService;
        this.achievementService = achievementService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(Long sessionId) {
        try {
            WorkoutSession session = sessionRepo.findById(sessionId).orElse(null);
            if (session == null || session.getUser() == null) {
                return;
            }
            session.setWorkoutExercises(
                    workoutExerciseRepo.findByWorkoutSession_IdOrderByExerciseOrderAsc(sessionId)
            );

            notificationService.handleWorkoutSessionCompleted(session);
            progressService.notifyProgressionSuggestions(session);
            achievementService.checkWorkoutAchievements(session.getUser());
            achievementService.checkXpAchievements(session.getUser());
        } catch (RuntimeException ex) {
            log.warn("Post-commit session completed hooks failed for session {}", sessionId, ex);
        }
    }
}
