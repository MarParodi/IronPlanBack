// src/main/java/com/example/ironplan/service/RoutineTemplateService.java
package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.*;
import com.example.ironplan.rest.dto.routine.RoutineOverviewResponse;
import com.example.ironplan.rest.dto.routine.RoutineBlockDto;
import com.example.ironplan.rest.dto.routine.RoutineBlockItemDto;
import com.example.ironplan.rest.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

@Service
public class RoutineTemplateService {

    private final RoutineTemplateRepository repo;
    private final RoutineBlockRepository blockRepo;
    private final ExerciseRepository exerciseRepo;
    private final UserUnlockedRoutineRepository unlockedRepo;
    private final UserRepository userRepo;
    private final AchievementService achievementService;


    public RoutineTemplateService(RoutineTemplateRepository repo,
                                  RoutineBlockRepository blockRepo,
                                  ExerciseRepository exerciseRepo,
                                  UserUnlockedRoutineRepository unlockedRepo,
                                  UserRepository userRepo,
                                  AchievementService achievementService) {
        this.repo = repo;
        this.blockRepo = blockRepo;
        this.exerciseRepo = exerciseRepo;
        this.unlockedRepo = unlockedRepo;
        this.userRepo = userRepo;
        this.achievementService = achievementService;
    }

    // ---------- CREAR RUTINA ----------
    @Transactional
    public CreateRoutineResponse create(User user, CreateRoutineRequest request) {
        // 1. Crear la rutina principal
        RoutineTemplate routine = new RoutineTemplate();
        routine.setName(request.name());
        routine.setDescription(request.description());
        routine.setLongDescription(request.longDescription() != null ? request.longDescription() : "");
        routine.setGoal(request.goal());
        routine.setSuggestedLevel(request.suggestedLevel());
        routine.setDays_per_week(request.daysPerWeek());
        routine.setDurationWeeks(request.durationWeeks());
        routine.setImg(request.img());
        routine.setIsPublic(request.isPublic());
        routine.setUser(user);
        routine.setCreatedAt(LocalDateTime.now());
        routine.setUsageCount(0);
        routine.setXp_cost(0);
        routine.setXp_gain(50); // XP base por completar
        routine.setRoutineGender(request.routineGender());


        // Determinar tipo y acceso según si es pública
        if (request.isPublic()) {
            routine.setType(Type.SHARED_COMMUNITY);
            routine.setAccess(Access_Type.USER_SHARED);
            routine.setStatus(RoutineStatus.PUBLISHED);
        } else {
            routine.setType(Type.USER_CREATED);
            routine.setAccess(Access_Type.FREE);
            routine.setStatus(RoutineStatus.DRAFT);
        }

        // Guardar la rutina primero para obtener ID
        routine = repo.save(routine);

        // 2. Crear los bloques
        int totalSeriesRoutine = 0;
        for (CreateBlockRequest blockReq : request.blocks()) {
            RoutineBlock block = new RoutineBlock();
            block.setRoutine(routine);
            block.setName(blockReq.name());
            block.setDescription(blockReq.description());
            block.setOrderIndex(blockReq.orderIndex());
            block.setDurationWeeks(blockReq.durationWeeks());

            // 3. Crear las sesiones del bloque
            for (CreateSessionRequest sessionReq : blockReq.sessions()) {
                RoutineDetail session = new RoutineDetail();
                session.setBlock(block);
                session.setTitle(sessionReq.title());
                session.setIcon(sessionReq.icon());
                session.setMuscles(sessionReq.muscles());
                session.setDescription(sessionReq.description());
                session.setSessionOrder(sessionReq.sessionOrder());

                // Calcular total series de la sesión
                int sessionTotalSeries = 0;

                // 4. Crear los ejercicios de la sesión
                for (CreateExerciseRequest exerciseReq : sessionReq.exercises()) {
                    Exercise exercise = exerciseRepo.findById(exerciseReq.exerciseId())
                            .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado: " + exerciseReq.exerciseId()));

                    RoutineExercise routineExercise = new RoutineExercise();
                    routineExercise.setExercise(exercise);
                    routineExercise.setDisplayName(exerciseReq.displayName() != null ? exerciseReq.displayName() : exercise.getName());
                    routineExercise.setExerciseOrder(exerciseReq.exerciseOrder());
                    routineExercise.setSets(exerciseReq.sets());
                    routineExercise.setRepsMin(exerciseReq.repsMin());
                    routineExercise.setRepsMax(exerciseReq.repsMax());
                    routineExercise.setRir(exerciseReq.rir());
                    routineExercise.setRestMinutes(exerciseReq.restMinutes());

                    session.addExercise(routineExercise);
                    sessionTotalSeries += exerciseReq.sets();
                }

                session.setTotalSeries(sessionTotalSeries);
                session.setEstimatedMinutes(sessionTotalSeries * 3); // ~3 min por serie
                session.setEstimatedXp(sessionTotalSeries * 5); // 5 XP por serie

                block.addSession(session);
                totalSeriesRoutine += sessionTotalSeries;
            }

            routine.addBlock(block);
        }

        // Guardar todo (cascade)
        repo.save(routine);

        // Verificar hazañas de creación de rutinas
        achievementService.checkRoutineCreationAchievements(user);

        return new CreateRoutineResponse(
                routine.getId(),
                routine.getName(),
                "Rutina creada exitosamente"
        );
    }

    public Page<RoutineListItemResponse> listMine(User user, Pageable pageable) {
        return repo
                .findByUser_Id(user.getId(), pageable)
                .map(this::toListItemResponse);
    }


    // ---------- Detalle "ligero" (para modal) ----------
    public RoutineTemplate getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Routine not found"));
    }

    public RoutineDetailResponse getDetail(Long id) {
        RoutineTemplate r = getById(id);

        return new RoutineDetailResponse(
                r.getId(),
                r.getName(),
                r.getGoal(),           // según tu DTO: String / Enum
                r.getAccess(),
                r.getImg(),
                r.getDescription(),
                r.getSuggestedLevel(),
                r.getXp_cost()
                // nivel sugerido para el modal
        );
    }

    // ---------- Listas ----------
    // Catálogo público publicado con filtros opcionales
    public Page<RoutineListItemResponse> listPublic(User user, String search, Goal goal, Integer daysPerWeek, RoutineGender routineGender, Pageable pageable) {
        Page<RoutineTemplate> routines = repo.findPublicWithFilters(RoutineStatus.PUBLISHED, search, goal, daysPerWeek, routineGender, pageable);
        
        // Obtener IDs de rutinas desbloqueadas por el usuario actual
        Set<Long> unlockedRoutineIds = new HashSet<>();
        if (user != null) {
            List<Long> routineIds = routines.getContent().stream()
                    .map(RoutineTemplate::getId)
                    .toList();
            unlockedRoutineIds = unlockedRepo.findUnlockedRoutineIdsByUserAndRoutineIds(user.getId(), routineIds);
        }
        
        final Set<Long> finalUnlockedIds = unlockedRoutineIds;
        return routines.map(r -> toListItemResponseWithUnlock(r, finalUnlockedIds.contains(r.getId())));
    }

    // Panel admin (todas)
    public Page<RoutineListItemResponse> listAll(Pageable pageable) {
        return repo.findAll(pageable)
                .map(this::toListItemResponse);
    }

    // Mapea RoutineTemplate a RoutineListItemResponse
    // Si el type es SHARED_COMMUNITY, cambia accessType a USER_SHARED y agrega ownerUsername
    private RoutineListItemResponse toListItemResponse(RoutineTemplate r) {
        return toListItemResponseWithUnlock(r, false);
    }

    // Mapea con información de desbloqueo
    private RoutineListItemResponse toListItemResponseWithUnlock(RoutineTemplate r, boolean unlockedByUser) {
        Access_Type accessType = r.getAccess();
        String ownerUsername = null;

        // Si es una rutina compartida por la comunidad, cambiar accessType y obtener username
        if (r.getType() == Type.SHARED_COMMUNITY && r.getUser() != null) {
            accessType = Access_Type.USER_SHARED;
            ownerUsername = r.getUser().getDisplayUsername();
        }

        return new RoutineListItemResponse(
                r.getId(),
                r.getName(),
                r.getGoal(),
                accessType,
                r.getImg(),
                r.getDescription(),
                ownerUsername,
                r.getUsageCount() != null ? r.getUsageCount() : 0,
                r.getRoutineGender(),
                r.getXp_cost(),
                unlockedByUser
        );
    }

    //Desbloquear rutinas con xp
    @Transactional
    public void unlockWithXp(User user, Long routineId) {
        RoutineTemplate routine = repo.findById(routineId)
                .orElseThrow(() -> new NotFoundException("Rutina no encontrada"));

        if (routine.getAccess() != Access_Type.XP_UNLOCK) {
            throw new IllegalStateException("Esta rutina no requiere XP");
        }

        // ¿Ya desbloqueada?
        if (unlockedRepo.existsByUser_IdAndRoutine_Id(user.getId(), routineId)) {
            return; // ya estaba desbloqueada
        }

        int cost = routine.getXp_cost();

        if (user.getXpPoints() < cost) {
            throw new IllegalStateException("XP insuficiente");
        }

        // Descontar XP
        user.setXpPoints(user.getXpPoints() - cost);
        userRepo.save(user);

        // Registrar desbloqueo
        UserUnlockedRoutine unlock = new UserUnlockedRoutine();
        unlock.setUser(user);
        unlock.setRoutine(routine);
        unlockedRepo.save(unlock);
    }


    // ---------- OVERVIEW grande (Rutina Híbrida) ----------
    public RoutineOverviewResponse getOverview(Long routineId) {
        RoutineTemplate r = getById(routineId);

        // Traer todos los bloques ordenados
        List<RoutineBlock> routineBlocks = blockRepo.findByRoutine_IdOrderByOrderIndexAsc(routineId);

        List<RoutineBlockDto> blocks = routineBlocks.stream()
                .map(block -> {
                    List<RoutineBlockItemDto> items = block.getSessions().stream()
                            .map(session -> new RoutineBlockItemDto(
                                    session.getId(),
                                    session.getTitle(),
                                    session.getTotalSeries(),
                                    session.getMuscles()
                            ))
                            .toList();

                    return new RoutineBlockDto(
                            block.getId(),
                            block.getOrderIndex(),
                            block.getName(),
                            block.getDescription(),
                            block.getDurationWeeks(),
                            items
                    );
                })
                .toList();

        return new RoutineOverviewResponse(
                r.getId(),
                r.getName(),
                r.getDurationWeeks(),
                r.getLongDescription(),
                r.getGoal() != null ? r.getGoal().name() : null,
                r.getSuggestedLevel() != null ? r.getSuggestedLevel().name() : null,
                r.getDays_per_week(),
                blocks
        );
    }
}
