package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProfileService {

    private final WorkoutSessionRepository workoutSessionRepo;
    private final WorkoutExerciseRepository workoutExerciseRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final RoutineTemplateRepository routineTemplateRepo;
    private final UserXpEventRepository userXpEventRepo;

    public ProfileService(
            WorkoutSessionRepository workoutSessionRepo,
            WorkoutExerciseRepository workoutExerciseRepo,
            WorkoutSetRepository workoutSetRepo,
            RoutineTemplateRepository routineTemplateRepo,
            UserXpEventRepository userXpEventRepo
    ) {
        this.workoutSessionRepo = workoutSessionRepo;
        this.workoutExerciseRepo = workoutExerciseRepo;
        this.workoutSetRepo = workoutSetRepo;
        this.routineTemplateRepo = routineTemplateRepo;
        this.userXpEventRepo = userXpEventRepo;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(User user) {

        // -------- HEADER --------
        String xpRankCode  = user.getXpRank() != null ? user.getXpRank().name() : null;
        String xpRankLabel = user.getXpRank() != null ? user.getXpRank().getDisplayName() : null;

        var header = new ProfileHeaderDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getLevel() != null ? user.getLevel().name() : null,
                user.getXpPoints() != null ? user.getXpPoints() : 0,
                user.getLifetimeXp() != null ? user.getLifetimeXp() : 0,
                xpRankCode,
                xpRankLabel,
                user.getCreatedAt()
        );

        // -------- STATS --------
        long totalWorkouts = workoutSessionRepo
                .countByUser_IdAndStatus(user.getId(), WorkoutSessionStatus.COMPLETED);

        long totalRoutinesOwned = routineTemplateRepo
                .countByUser_Id(user.getId());

        long totalXpActions = userXpEventRepo
                .countByUser_Id(user.getId());

        var stats = new ProfileStatsDto(
                totalWorkouts,
                totalRoutinesOwned,
                totalXpActions
        );

        // -------- ÚLTIMOS REGISTROS --------
        var recentSessions = workoutSessionRepo
                .findTop5ByUser_IdAndStatusOrderByStartedAtDesc(
                        user.getId(),
                        WorkoutSessionStatus.COMPLETED
                );

        List<RecentWorkoutDto> recent = new ArrayList<>();

        for (WorkoutSession session : recentSessions) {
            // nombre
            String routineName = session.getRoutineDetail() != null
                    ? session.getRoutineDetail().getTitle()
                    : "Sesión de entrenamiento";

            // duración
            long minutes = 0;
            if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                minutes = Duration.between(
                        session.getStartedAt(),
                        session.getCompletedAt()
                ).toMinutes();
            }

            // series y peso total (sencillo: contar sets y sumar weight*reps)
            int totalSeries = 0;
            double totalWeightKg = 0.0;

            var exercises = workoutExerciseRepo
                    .findByWorkoutSession_IdOrderByExerciseOrderAsc(session.getId());

            for (WorkoutExercise ex : exercises) {
                var sets = workoutSetRepo
                        .findByWorkoutExercise_IdOrderBySetNumberAsc(ex.getId());

                totalSeries += sets.size();

                for (WorkoutSet set : sets) {
                    int reps = set.getReps() != null ? set.getReps() : 0;
                    double w  = set.getWeightKg() != null ? set.getWeightKg() : 0.0;
                    totalWeightKg += w * reps;
                }
            }

            recent.add(new RecentWorkoutDto(
                    session.getId(),
                    routineName,
                    session.getStartedAt(),
                    totalSeries,
                    totalWeightKg,
                    minutes
            ));
        }

        return new ProfileResponse(header, stats, recent);
    }
}
