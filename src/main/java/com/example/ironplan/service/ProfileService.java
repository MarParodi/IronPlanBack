package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.*;
import com.example.ironplan.rest.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProfileService {

    private final WorkoutSessionRepository workoutSessionRepo;
    private final WorkoutExerciseRepository workoutExerciseRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final RoutineTemplateRepository routineTemplateRepo;
    private final RoutineBlockRepository routineBlockRepo;
    private final RoutineDetailRepository routineDetailRepo;
    private final UserXpEventRepository userXpEventRepo;
    private final UserRepository userRepo;

    public ProfileService(
            WorkoutSessionRepository workoutSessionRepo,
            WorkoutExerciseRepository workoutExerciseRepo,
            WorkoutSetRepository workoutSetRepo,
            RoutineTemplateRepository routineTemplateRepo,
            RoutineBlockRepository routineBlockRepo,
            RoutineDetailRepository routineDetailRepo,
            UserXpEventRepository userXpEventRepo,
            UserRepository userRepo
    ) {
        this.workoutSessionRepo = workoutSessionRepo;
        this.workoutExerciseRepo = workoutExerciseRepo;
        this.workoutSetRepo = workoutSetRepo;
        this.routineTemplateRepo = routineTemplateRepo;
        this.routineBlockRepo = routineBlockRepo;
        this.routineDetailRepo = routineDetailRepo;
        this.userXpEventRepo = userXpEventRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(User user) {

        // -------- HEADER --------
        String xpRankCode  = user.getXpRank() != null ? user.getXpRank().name() : null;
        String xpRankLabel = user.getXpRank() != null ? user.getXpRank().getDisplayName() : null;

        var header = new ProfileHeaderDto(
                user.getId(),
                user.getDisplayUsername(),  // El username real, no el email
                user.getEmail(),
                user.getLevel() != null ? user.getLevel().name() : null,
                user.getXpPoints() != null ? user.getXpPoints() : 0,
                user.getLifetimeXp() != null ? user.getLifetimeXp() : 0,
                xpRankCode,
                xpRankLabel,
                user.getCreatedAt(),
                user.getProfilePictureUrl()
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

        List<RecentWorkoutDto> recent = new ArrayList<>(recentSessions.size());
        for (WorkoutSession session : recentSessions) {
            recent.add(toRecentWorkoutDto(session));
        }

        return new ProfileResponse(header, stats, recent);

    }



    //Historial de entrenamientos

    private RecentWorkoutDto toRecentWorkoutDto(WorkoutSession session) {

        // nombre
        String routineName = session.getRoutineDetail() != null
                ? session.getRoutineDetail().getTitle()
                : "Sesión de entrenamiento";

        // duración
        long minutes = 0;
        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
            minutes = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMinutes();
        }

        // series y peso total (contar sets y sumar weight*reps)
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

        // ⚠️ tu constructor actual es:
        // (id, routineName, startedAt, totalSeries, totalWeightKg, minutes)
        return new RecentWorkoutDto(
                session.getId(),
                routineName,
                session.getStartedAt(),
                totalSeries,
                totalWeightKg,
                minutes
        );
    }

    @Transactional(readOnly = true)
    public List<RecentWorkoutDto> getWorkoutHistory(User user) {

        var sessions = workoutSessionRepo.findByUser_IdAndStatusOrderByCompletedAtDesc(
                user.getId(),
                WorkoutSessionStatus.COMPLETED
        );

        List<RecentWorkoutDto> history = new ArrayList<>(sessions.size());
        for (WorkoutSession session : sessions) {
            history.add(toRecentWorkoutDto(session));
        }

        return history;
    }



    // -------- RUTINA ACTUAL --------

    /**
     * Asigna una rutina al usuario como su rutina actual
     * e incrementa el contador de usos de la rutina
     */
    @Transactional
    public void startRoutine(User user, Long routineId) {
        RoutineTemplate routine = routineTemplateRepo.findById(routineId)
                .orElseThrow(() -> new NotFoundException("Rutina no encontrada: " + routineId));

        // Incrementar el contador de usos de la rutina
        Integer currentUsage = routine.getUsageCount() != null ? routine.getUsageCount() : 0;
        routine.setUsageCount(currentUsage + 1);
        routineTemplateRepo.save(routine);

        user.setCurrentRoutine(routine);
        user.setRoutineStartedAt(LocalDateTime.now());
        userRepo.save(user);
    }

    /**
     * Quita la rutina actual del usuario
     */
    @Transactional
    public void stopRoutine(User user) {
        user.setCurrentRoutine(null);
        user.setRoutineStartedAt(null);
        userRepo.save(user);
    }

    /**
     * Obtiene la rutina actual del usuario (si tiene una)
     */
    @Transactional(readOnly = true)
    public CurrentRoutineResponse getCurrentRoutine(User user) {
        RoutineTemplate routine = user.getCurrentRoutine();
        if (routine == null) {
            return null;
        }

        return new CurrentRoutineResponse(
                routine.getId(),
                routine.getName(),
                routine.getDescription(),
                routine.getGoal() != null ? routine.getGoal().name() : null,
                routine.getSuggestedLevel() != null ? routine.getSuggestedLevel().name() : null,
                routine.getDays_per_week(),
                routine.getDurationWeeks(),
                routine.getImg(),
                user.getRoutineStartedAt()
        );
    }

    /**
     * Obtiene la rutina activa con progreso completo (para la pantalla Mi Rutina)
     */
    @Transactional(readOnly = true)
    public ActiveRoutineResponse getActiveRoutineWithProgress(User user) {
        RoutineTemplate routine = user.getCurrentRoutine();
        if (routine == null) {
            return null;
        }

        // Obtener todos los bloques de la rutina ordenados
        List<RoutineBlock> routineBlocks = routineBlockRepo
                .findByRoutine_IdOrderByOrderIndexAsc(routine.getId());

        // Obtener sesiones completadas por el usuario para esta rutina
        List<WorkoutSession> completedWorkouts = workoutSessionRepo
                .findByUser_IdAndRoutineDetail_Block_Routine_IdAndStatus(
                        user.getId(),
                        routine.getId(),
                        WorkoutSessionStatus.COMPLETED
                );

        // Crear un set de IDs de sesiones completadas
        var completedSessionIds = completedWorkouts.stream()
                .map(ws -> ws.getRoutineDetail().getId())
                .collect(java.util.stream.Collectors.toSet());

        // Crear bloques con sesiones
        List<ActiveRoutineBlockDto> blocks = new ArrayList<>();
        int totalSessions = 0;
        int completedSessions = 0;

        for (RoutineBlock block : routineBlocks) {
            List<ActiveRoutineSessionDto> sessionDtos = new ArrayList<>();
            
            for (RoutineDetail detail : block.getSessions()) {
                boolean isCompleted = completedSessionIds.contains(detail.getId());
                
                // Buscar fecha de completado si existe
                LocalDateTime completedAt = null;
                if (isCompleted) {
                    completedAt = completedWorkouts.stream()
                            .filter(ws -> ws.getRoutineDetail().getId().equals(detail.getId()))
                            .map(WorkoutSession::getCompletedAt)
                            .findFirst()
                            .orElse(null);
                }

                sessionDtos.add(new ActiveRoutineSessionDto(
                        detail.getId(),
                        detail.getTitle(),
                        detail.getTotalSeries() != null ? detail.getTotalSeries() : 0,
                        detail.getMuscles(),
                        detail.getSessionOrder(),
                        isCompleted,
                        completedAt
                ));

                totalSessions++;
                if (isCompleted) completedSessions++;
            }

            blocks.add(new ActiveRoutineBlockDto(
                    block.getId(),
                    block.getOrderIndex(),
                    block.getName(),
                    block.getDescription(),
                    block.getDurationWeeks(),
                    sessionDtos
            ));
        }

        // Calcular porcentaje de progreso
        int progressPercent = totalSessions > 0 
                ? (int) Math.round((completedSessions * 100.0) / totalSessions) 
                : 0;

        return new ActiveRoutineResponse(
                routine.getId(),
                routine.getName(),
                routine.getDurationWeeks(),
                routine.getDays_per_week(),
                totalSessions,
                completedSessions,
                progressPercent,
                user.getRoutineStartedAt(),
                blocks
        );
    }


    /**
     * Reordena las sesiones dentro de un bloque de la rutina activa del usuario
     */
    @Transactional
    public void reorderSessions(User user, Long routineId, Long blockId, List<Long> sessionIds) {
        // Verificar que el usuario tiene esta rutina activa
        RoutineTemplate currentRoutine = user.getCurrentRoutine();
        if (currentRoutine == null || !currentRoutine.getId().equals(routineId)) {
            throw new NotFoundException("No tienes esta rutina activa");
        }

        // Obtener el bloque verificando que pertenece a la rutina
        RoutineBlock block = routineBlockRepo.findByIdAndRoutine_Id(blockId, routineId)
                .orElseThrow(() -> new NotFoundException("Bloque no encontrado"));

        // Obtener las sesiones del bloque
        List<RoutineDetail> sessions = block.getSessions();

        if (sessions.isEmpty()) {
            throw new NotFoundException("El bloque no tiene sesiones");
        }

        // Verificar que todos los IDs enviados corresponden a sesiones del bloque
        var sessionMap = sessions.stream()
                .collect(java.util.stream.Collectors.toMap(RoutineDetail::getId, s -> s));

        for (Long sessionId : sessionIds) {
            if (!sessionMap.containsKey(sessionId)) {
                throw new IllegalArgumentException("Sesión " + sessionId + " no pertenece a este bloque");
            }
        }

        // Actualizar el orden
        int newOrder = 1;
        for (Long sessionId : sessionIds) {
            RoutineDetail session = sessionMap.get(sessionId);
            session.setSessionOrder(newOrder);
            routineDetailRepo.save(session);
            newOrder++;
        }
    }
}
