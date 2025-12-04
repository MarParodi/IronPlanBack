// src/main/java/com/example/ironplan/service/RoutineTemplateService.java
package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.ExerciseRepository;
import com.example.ironplan.repository.RoutineTemplateRepository;
import com.example.ironplan.repository.RoutineDetailRepository;
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

@Service
public class RoutineTemplateService {

    private final RoutineTemplateRepository repo;
    private final RoutineDetailRepository detailRepo;
    private final ExerciseRepository exerciseRepo;

    public RoutineTemplateService(RoutineTemplateRepository repo,
                                  RoutineDetailRepository detailRepo,
                                  ExerciseRepository exerciseRepo) {
        this.repo = repo;
        this.detailRepo = detailRepo;
        this.exerciseRepo = exerciseRepo;
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

        // 2. Crear las sesiones
        int totalSeriesRoutine = 0;
        for (CreateSessionRequest sessionReq : request.sessions()) {
            RoutineDetail session = new RoutineDetail();
            session.setRoutine(routine);
            session.setTitle(sessionReq.title());
            session.setIcon(sessionReq.icon());
            session.setMuscles(sessionReq.muscles());
            session.setDescription(sessionReq.description());
            session.setBlockNumber(sessionReq.blockNumber());
            session.setBlockLabel(sessionReq.blockLabel());
            session.setOrderInBlock(sessionReq.orderInBlock());
            session.setSessionOrder(sessionReq.orderInBlock());

            // Calcular total series de la sesión
            int sessionTotalSeries = 0;

            // 3. Crear los ejercicios de la sesión
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

            routine.getSessions().add(session);
            totalSeriesRoutine += sessionTotalSeries;
        }

        // Guardar todo (cascade)
        repo.save(routine);

        return new CreateRoutineResponse(
                routine.getId(),
                routine.getName(),
                "Rutina creada exitosamente"
        );
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
                r.getAccess(),         // idem
                r.getDescription(),
                r.getSuggestedLevel()  // nivel sugerido para el modal
        );
    }

    // ---------- Listas ----------
    // Catálogo público publicado con filtros opcionales
    public Page<RoutineListItemResponse> listPublic(String search, Goal goal, Integer daysPerWeek, Pageable pageable) {
        return repo.findPublicWithFilters(RoutineStatus.PUBLISHED, search, goal, daysPerWeek, pageable)
                .map(this::toListItemResponse);
    }

    // Panel admin (todas)
    public Page<RoutineListItemResponse> listAll(Pageable pageable) {
        return repo.findAll(pageable)
                .map(this::toListItemResponse);
    }

    // Mapea RoutineTemplate a RoutineListItemResponse
    // Si el type es SHARED_COMMUNITY, cambia accessType a USER_SHARED y agrega ownerUsername
    private RoutineListItemResponse toListItemResponse(RoutineTemplate r) {
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
                r.getUsageCount() != null ? r.getUsageCount() : 0
        );
    }

    // ---------- OVERVIEW grande (Rutina Híbrida) ----------
    public RoutineOverviewResponse getOverview(Long routineId) {
        RoutineTemplate r = getById(routineId);

        // Traer todas las sesiones (RoutineDetail) ordenadas
        List<RoutineDetail> details = detailRepo
                .findByRoutine_IdOrderByBlockNumberAscOrderInBlockAsc(routineId);

        // Agrupar por bloque
        Map<Integer, List<RoutineDetail>> grouped =
                details.stream().collect(Collectors.groupingBy(
                        RoutineDetail::getBlockNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RoutineBlockDto> blocks = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            int blockNumber = entry.getKey();
            List<RoutineDetail> sessions = entry.getValue();

            String blockLabel = sessions.get(0).getBlockLabel(); // Semana 1–12, etc.

            List<RoutineBlockItemDto> items = sessions.stream()
                    .map(d -> new RoutineBlockItemDto(
                            d.getId(),
                            d.getTitle(),
                            d.getTotalSeries(),
                            d.getMuscles()
                    ))
                    .toList();

            blocks.add(new RoutineBlockDto(
                    blockNumber,
                    "Bloque " + blockNumber + " — " + blockLabel,
                    items
            ));
        }

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

