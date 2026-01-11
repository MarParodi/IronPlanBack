// src/main/java/com/example/ironplan/service/WorkoutSessionService.java
package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.PreviousSessionComparison;
import com.example.ironplan.rest.dto.WorkoutSessionSummaryResponse;
import com.example.ironplan.rest.dto.WorkoutSessionDetailResponse;
import com.example.ironplan.rest.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkoutSessionService {

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutExerciseRepository workoutExerciseRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final RoutineDetailRepository routineDetailRepo;
    private final UserRepository userRepo;
    private final AchievementService achievementService;
private final ExerciseRepository exerciseRepo;

    public WorkoutSessionService(
            WorkoutSessionRepository sessionRepo,
            WorkoutExerciseRepository workoutExerciseRepo,
            WorkoutSetRepository workoutSetRepo,
            RoutineDetailRepository routineDetailRepo,
            UserRepository userRepo,
            ExerciseRepository exerciseRepo,
            AchievementService achievementService
    ) {
        this.sessionRepo = sessionRepo;
        this.workoutExerciseRepo = workoutExerciseRepo;
        this.workoutSetRepo = workoutSetRepo;
        this.routineDetailRepo = routineDetailRepo;
        this.userRepo = userRepo;
        this.achievementService = achievementService;
        this.exerciseRepo = exerciseRepo;
    }

    @Transactional
    public WorkoutSession skipSession(Long userId, Long routineDetailId) {
        // 1) crear sesión real basada en RoutineDetail (igual que startSession)
        WorkoutSession session = startSession(userId, routineDetailId);

        // 2) marcar como "saltada" usando CANCELLED
        session.setStatus(WorkoutSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setProgressPercentage(0.0);

        return sessionRepo.save(session);
    }

    /**
     * Inicia una sesión de entrenamiento real para un usuario,
     * basada en una RoutineDetail (sesión "Tirón", "Empuje", etc.).
     */
    @Transactional
    public WorkoutSession startSession(Long userId, Long routineDetailId) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + userId));

        var detail = routineDetailRepo.findById(routineDetailId)
                .orElseThrow(() -> new NotFoundException("Sesión de rutina no encontrada: " + routineDetailId));

        var session = new WorkoutSession();
        session.setUser(user);
        session.setRoutineDetail(detail);
        session.setStatus(WorkoutSessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setXpEarned(0);
        session.setCompletedExercises(0);
        session.setProgressPercentage(0.0);

        var routineExercises = detail.getExercises();
        int totalExercises = routineExercises != null ? routineExercises.size() : 0;
        session.setTotalExercises(totalExercises);

        // guardamos para obtener ID
        session = sessionRepo.save(session);

        var workoutExercises = new ArrayList<WorkoutExercise>();
        if (routineExercises != null) {
            for (RoutineExercise re : routineExercises) {
                var we = new WorkoutExercise();
                we.setWorkoutSession(session);
                we.setRoutineExercise(re);

                var displayName = re.getDisplayName();
                if (displayName == null && re.getExercise() != null) {
                    displayName = re.getExercise().getName();
                }
                we.setExerciseName(displayName != null ? displayName : "Ejercicio");

                we.setExerciseOrder(re.getExerciseOrder());
                we.setPlannedSets(re.getSets());
                we.setPlannedRepsMin(re.getRepsMin());
                we.setPlannedRepsMax(re.getRepsMax());
                we.setPlannedRir(re.getRir());
                // si re.getRestMinutes() son minutos, aquí probablemente quieres * 60
                we.setPlannedRestSeconds(re.getRestMinutes());

                we.setStatus(WorkoutExerciseStatus.PENDING);
                we.setCompletedSets(0);

                workoutExercises.add(we);
            }
        }

        // guardamos los ejercicios
        workoutExerciseRepo.saveAll(workoutExercises);

        session.setWorkoutExercises(workoutExercises);

        return session;
    }

    //metodo para sesion personalizada
    @Transactional
    public WorkoutSession startCustomSession(Long userId, com.example.ironplan.rest.dto.StartCustomWorkoutRequest request) {

        var user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + userId));

        var session = new WorkoutSession();
        session.setUser(user);
        session.setRoutineDetail(null); // custom
        session.setStatus(WorkoutSessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setXpEarned(0);
        session.setCompletedExercises(0);
        session.setProgressPercentage(0.0);

        int totalExercises = request.exercises() != null ? request.exercises().size() : 0;
        session.setTotalExercises(totalExercises);

        session = sessionRepo.save(session);

        // Defaults (porque planned_* son NOT NULL en tu entidad)
        final int DEFAULT_SETS = 3;
        final int DEFAULT_REPS_MIN = 8;
        final int DEFAULT_REPS_MAX = 12;
        final int DEFAULT_REST_SECONDS = 60;

        var workoutExercises = new ArrayList<WorkoutExercise>();

        int autoOrder = 1;

        for (var item : request.exercises()) {

            var catalogExercise = exerciseRepo.findById(item.exerciseId())
                    .orElseThrow(() -> new NotFoundException("Ejercicio de catálogo no encontrado: " + item.exerciseId()));

            var we = new WorkoutExercise();
            we.setWorkoutSession(session);

            // Custom: no viene de rutina
            we.setRoutineExercise(null);
            we.setExercise(catalogExercise);

            we.setExerciseName(catalogExercise.getName() != null ? catalogExercise.getName() : "Ejercicio");

            Integer order = item.orderIndex() != null ? item.orderIndex() : autoOrder++;
            we.setExerciseOrder(order);

            we.setPlannedSets(item.plannedSets() != null ? item.plannedSets() : DEFAULT_SETS);
            we.setPlannedRepsMin(item.plannedRepsMin() != null ? item.plannedRepsMin() : DEFAULT_REPS_MIN);
            we.setPlannedRepsMax(item.plannedRepsMax() != null ? item.plannedRepsMax() : DEFAULT_REPS_MAX);

            we.setPlannedRir(item.plannedRir());
            we.setPlannedRestSeconds(item.plannedRestSeconds() != null ? item.plannedRestSeconds() : DEFAULT_REST_SECONDS);

            we.setStatus(WorkoutExerciseStatus.PENDING);
            we.setCompletedSets(0);

            workoutExercises.add(we);
        }

        workoutExerciseRepo.saveAll(workoutExercises);
        session.setWorkoutExercises(workoutExercises);

        return session;
    }

    @Transactional(readOnly = true)
    public WorkoutSessionDetailResponse getSessionDetail(Long sessionId, Long userId) {

        WorkoutSession session = getSessionForUser(sessionId, userId);
        var routineDetail = session.getRoutineDetail();

        // nombre visible
        String routineName = (routineDetail != null && routineDetail.getTitle() != null)
                ? routineDetail.getTitle()
                : "Entrenamiento";

        // duración
        long durationMinutes = 0;
        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
            durationMinutes = java.time.Duration
                    .between(session.getStartedAt(), session.getCompletedAt())
                    .toMinutes();
        }

        // ejercicios ordenados
        var exercises = workoutExerciseRepo
                .findByWorkoutSession_IdOrderByExerciseOrderAsc(sessionId);

        int totalSeries = 0;
        double totalWeightKg = 0.0;

        var exerciseDtos = new java.util.ArrayList<com.example.ironplan.rest.dto.WorkoutExerciseDetailDto>();

        for (WorkoutExercise ex : exercises) {

            var sets = workoutSetRepo.findByWorkoutExercise_IdOrderBySetNumberAsc(ex.getId());

            totalSeries += sets.size();

            var setDtos = new java.util.ArrayList<com.example.ironplan.rest.dto.WorkoutSetDetailDto>();
            for (WorkoutSet s : sets) {
                int reps = s.getReps() != null ? s.getReps() : 0;
                double w  = s.getWeightKg() != null ? s.getWeightKg() : 0.0;
                totalWeightKg += (w * reps);

                setDtos.add(new com.example.ironplan.rest.dto.WorkoutSetDetailDto(
                        s.getId(),
                        s.getSetNumber(),
                        s.getReps(),
                        s.getWeightKg(),
                        s.isCompleted(),
                        s.getNotes()
                ));
            }

            exerciseDtos.add(new com.example.ironplan.rest.dto.WorkoutExerciseDetailDto(
                    ex.getId(),
                    ex.getExerciseOrder(),
                    ex.getExerciseName(),
                    ex.getPlannedSets(),
                    ex.getPlannedRepsMin(),
                    ex.getPlannedRepsMax(),
                    ex.getPlannedRir(),
                    ex.getPlannedRestSeconds(),
                    ex.getStatus() != null ? ex.getStatus().name() : null,
                    ex.getCompletedSets(),
                    setDtos
            ));
        }

        return new com.example.ironplan.rest.dto.WorkoutSessionDetailResponse(
                session.getId(),
                routineName,
                session.getStartedAt(),
                session.getCompletedAt(),
                durationMinutes,
                totalSeries,
                totalWeightKg,
                session.getXpEarned(),
                exerciseDtos
        );
    }

    /**
     * Obtiene una sesión por id, asegurando que pertenece al usuario dado.
     * (útil para que nadie “toque” la sesión de otro).
     */
    @Transactional(readOnly = true)
    public WorkoutSession getSessionForUser(Long sessionId, Long userId) {
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesión de entrenamiento no encontrada: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new NotFoundException("La sesión no pertenece al usuario.");
            // si más adelante quieres manejar una excepción de acceso, creamos otra
        }

        return session;
    }

    /**
     * Marca la sesión como completada, actualizando estado y fecha.
     * También verifica si el usuario desbloqueó nuevas hazañas.
     */
    @Transactional
    public void completeSession(Long sessionId, Long userId) {
        var session = getSessionForUser(sessionId, userId);
        session.setStatus(WorkoutSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setProgressPercentage(100.0);
        sessionRepo.save(session);

        // Verificar hazañas de entrenamiento
        achievementService.checkWorkoutAchievements(session.getUser());
        
        // Verificar hazañas de XP (por si ganó XP en este entrenamiento)
        achievementService.checkXpAchievements(session.getUser());
    }

    /**
     * Descarta una sesión activa sin guardar el progreso.
     * La marca como CANCELLED y elimina los sets registrados.
     */
    @Transactional
    public void discardSession(Long sessionId, Long userId) {
        var session = getSessionForUser(sessionId, userId);
        
        // Solo se puede descartar sesiones activas
        if (session.getStatus() != WorkoutSessionStatus.ACTIVE) {
            throw new IllegalStateException("Solo se pueden descartar sesiones activas");
        }
        
        // Eliminar todos los sets registrados
        var exercises = workoutExerciseRepo.findByWorkoutSession_IdOrderByExerciseOrderAsc(sessionId);
        for (WorkoutExercise exercise : exercises) {
            workoutSetRepo.deleteAllByWorkoutExercise_Id(exercise.getId());
        }
        
        // Marcar la sesión como cancelada
        session.setStatus(WorkoutSessionStatus.CANCELLED);
        session.setCompletedAt(LocalDateTime.now());
        session.setProgressPercentage(0.0);
        session.setXpEarned(0);
        sessionRepo.save(session);
    }

    /**
     * Finaliza una sesión guardando el progreso actual.
     * Útil cuando el usuario quiere terminar antes de completar todos los ejercicios.
     */
    @Transactional
    public void finishSession(Long sessionId, Long userId) {
        var session = getSessionForUser(sessionId, userId);
        
        // Solo se puede finalizar sesiones activas
        if (session.getStatus() != WorkoutSessionStatus.ACTIVE) {
            throw new IllegalStateException("Solo se pueden finalizar sesiones activas");
        }
        
        // Calcular progreso real basado en ejercicios completados
        var exercises = workoutExerciseRepo.findByWorkoutSession_IdOrderByExerciseOrderAsc(sessionId);
        int completedExercises = 0;
        
        for (WorkoutExercise exercise : exercises) {
            long setsCompleted = workoutSetRepo.countByWorkoutExercise_IdAndCompletedTrue(exercise.getId());
            if (setsCompleted > 0) {
                completedExercises++;
                exercise.setCompletedSets((int) setsCompleted);
                exercise.setStatus(WorkoutExerciseStatus.COMPLETED);
            }
        }
        workoutExerciseRepo.saveAll(exercises);
        
        // Actualizar sesión
        session.setCompletedExercises(completedExercises);
        double progress = session.getTotalExercises() > 0 
            ? (completedExercises * 100.0) / session.getTotalExercises() 
            : 0.0;
        session.setProgressPercentage(progress);
        session.setStatus(WorkoutSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepo.save(session);

        // Verificar hazañas si completó al menos un ejercicio
        if (completedExercises > 0) {
            achievementService.checkWorkoutAchievements(session.getUser());
            achievementService.checkXpAchievements(session.getUser());
        }
    }

    @Transactional
    public void reorderNextExercises(Long sessionId, Long userId, List<Long> workoutExerciseIds) {
        // 1) Asegurarnos de que la sesión existe y es del usuario
        WorkoutSession session = getSessionForUser(sessionId, userId);

        List<WorkoutExercise> allExercises = session.getWorkoutExercises();
        if (allExercises == null || allExercises.isEmpty()) {
            throw new NotFoundException("La sesión no tiene ejercicios configurados.");
        }

        // 2) Determinar el ejercicio “actual”
        //    - Primero intentamos encontrar uno con estado ACTIVE
        //    - Si no hay, usamos el de menor exerciseOrder
        Integer currentOrder = allExercises.stream()
                .map(WorkoutExercise::getExerciseOrder)
                .findFirst()
                .orElse(
                        allExercises.stream()
                                .map(WorkoutExercise::getExerciseOrder)
                                .min(Integer::compareTo)
                                .orElse(0)
                );

        // 3) Mapear todos los ejercicios de la sesión por ID
        Map<Long, WorkoutExercise> byId = allExercises.stream()
                .collect(Collectors.toMap(WorkoutExercise::getId, Function.identity()));

        // 4) Validar que TODOS los ids que mandó el front están en la sesión
        for (Long id : workoutExerciseIds) {
            if (!byId.containsKey(id)) {
                throw new IllegalArgumentException("El ejercicio " + id + " no pertenece a la sesión");
            }
        }

        // 5) Reenumerar solo los "siguientes" a partir de currentOrder + 1
        int newOrder = currentOrder + 1;

        for (Long id : workoutExerciseIds) {
            WorkoutExercise we = byId.get(id);
            we.setExerciseOrder(newOrder++);
        }

        // 6) Guardar cambios
        workoutExerciseRepo.saveAll(allExercises);
    }

    /**
     * Obtiene el resumen de una sesión completada.
     */
    @Transactional(readOnly = true)
    public WorkoutSessionSummaryResponse getSessionSummary(Long sessionId, Long userId) {
        var session = getSessionForUser(sessionId, userId);
        var user = session.getUser();
        var routineDetail = session.getRoutineDetail();
        
        // Calcular duración
        LocalDateTime startedAt = session.getStartedAt();
        LocalDateTime completedAt = session.getCompletedAt() != null 
                ? session.getCompletedAt() 
                : LocalDateTime.now();
        
        long durationSeconds = Duration.between(startedAt, completedAt).getSeconds();
        String durationFormatted = formatDuration(durationSeconds);
        
        // Contar series completadas
        var exercises = workoutExerciseRepo.findByWorkoutSession_IdOrderByExerciseOrderAsc(sessionId);
        int totalSeries = exercises.stream()
                .mapToInt(e -> e.getPlannedSets() != null ? e.getPlannedSets() : 0)
                .sum();
        int completedSeries = exercises.stream()
                .mapToInt(e -> e.getCompletedSets() != null ? e.getCompletedSets() : 0)
                .sum();
        
        // Buscar sesión anterior para comparación
        PreviousSessionComparison previousComparison = null;
        if (routineDetail != null) {
            var previousSession = sessionRepo.findFirstByUser_IdAndRoutineDetail_IdAndStatusAndIdNotOrderByCompletedAtDesc(
                    userId,
                    routineDetail.getId(),
                    WorkoutSessionStatus.COMPLETED,
                    sessionId
            );
            
            if (previousSession.isPresent()) {
                var prev = previousSession.get();
                long prevDuration = 0;
                if (prev.getStartedAt() != null && prev.getCompletedAt() != null) {
                    prevDuration = Duration.between(prev.getStartedAt(), prev.getCompletedAt()).getSeconds();
                }
                
                previousComparison = new PreviousSessionComparison(
                        prev.getId(),
                        prev.getCompletedAt(),
                        prevDuration,
                        prev.getXpEarned() != null ? prev.getXpEarned() : 0,
                        durationSeconds - prevDuration,  // positivo = tardaste más
                        (session.getXpEarned() != null ? session.getXpEarned() : 0) 
                                - (prev.getXpEarned() != null ? prev.getXpEarned() : 0)
                );
            }
        }
        
        return new WorkoutSessionSummaryResponse(
                session.getId(),
                routineDetail != null ? routineDetail.getTitle() : "Entrenamiento",
                routineDetail != null ? routineDetail.getIcon() : null,
                routineDetail != null ? routineDetail.getMuscles() : null,
                startedAt,
                completedAt,
                durationSeconds,
                durationFormatted,
                session.getTotalExercises(),
                session.getCompletedExercises(),
                totalSeries,
                completedSeries,
                session.getProgressPercentage(),
                session.getXpEarned(),
                user.getXpPoints(),
                user.getXpRank() != null ? user.getXpRank().name() : "NOVATO_I",
                previousComparison
        );
    }
    
    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
