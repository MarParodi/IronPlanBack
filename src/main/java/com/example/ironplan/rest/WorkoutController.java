package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.model.WorkoutExercise;
import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.model.WorkoutSet;
import com.example.ironplan.repository.WorkoutExerciseRepository;
import com.example.ironplan.repository.WorkoutSetRepository;
import com.example.ironplan.rest.dto.*;
import com.example.ironplan.rest.mapper.WorkoutExerciseViewMapper;
import com.example.ironplan.rest.error.NotFoundException;
import com.example.ironplan.service.WorkoutExerciseService;
import com.example.ironplan.service.WorkoutSessionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutSessionService workoutSessionService;
    private final WorkoutExerciseService workoutExerciseService;
    private final WorkoutExerciseRepository workoutExerciseRepo;
    private final WorkoutSetRepository workoutSetRepo;

    public WorkoutController(
            WorkoutSessionService workoutSessionService,
            WorkoutExerciseService workoutExerciseService,
            WorkoutExerciseRepository workoutExerciseRepo,
            WorkoutSetRepository workoutSetRepo
    ) {
        this.workoutSessionService = workoutSessionService;
        this.workoutExerciseService = workoutExerciseService;
        this.workoutExerciseRepo = workoutExerciseRepo;
        this.workoutSetRepo = workoutSetRepo;
    }

    // 1) INICIAR SESIÓN DE ENTRENAMIENTO (desde rutina)
    @PostMapping("/start")
    public ResponseEntity<WorkoutExerciseDetailResponse> startWorkout(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid StartWorkoutRequest request
    ) {
        // crear sesión real a partir de RoutineDetail
        WorkoutSession session = workoutSessionService.startSession(
                user.getId(),
                request.routineDetailId()
        );

        // obtener el primer ejercicio de la sesión
        WorkoutExercise firstExercise = workoutExerciseRepo
                .findByWorkoutSession_IdOrderByExerciseOrderAsc(session.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "La sesión no tiene ejercicios configurados."
                ));

        // última serie completada
        Long catalogExerciseId = resolveCatalogExerciseId(firstExercise);

        WorkoutSet previousSet = workoutSetRepo
                .findLastCompletedSetForUserAndExercise(
                        user.getId(),
                        catalogExerciseId,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);



        WorkoutExerciseDetailResponse response =
                WorkoutExerciseViewMapper.toDetailResponse(session, firstExercise, previousSet);

        return ResponseEntity.ok(response);
    }

    // 1.5) INICIAR SESIÓN PERSONALIZADA (sin rutina, desde catálogo)
    @PostMapping("/custom/start")
    public ResponseEntity<StartCustomWorkoutResponse> startCustomWorkout(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid StartCustomWorkoutRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        WorkoutSession session = workoutSessionService.startCustomSession(
                user.getId(),
                request
        );

        return ResponseEntity.ok(new StartCustomWorkoutResponse(session.getId()));
    }

    // 2) OBTENER EJERCICIO ACTUAL POR ORDEN
    @GetMapping("/{sessionId}/exercise/{order}")
    public ResponseEntity<WorkoutExerciseDetailResponse> getExerciseByOrder(
            @PathVariable Long sessionId,
            @PathVariable Integer order,
            @AuthenticationPrincipal User user
    ) {
        // obtiene la sesión y valida que pertenece al usuario
        WorkoutSession session = workoutSessionService.getSessionForUser(sessionId, user.getId());

        // obtiene el ejercicio por orden
        WorkoutExercise exercise = workoutExerciseService
                .getExerciseForUserByOrder(sessionId, user.getId(), order);

        // última serie completada de ese ejercicio (para "serie anterior")
        Long catalogExerciseId = resolveCatalogExerciseId(exercise);

        WorkoutSet previousSet = workoutSetRepo
                .findLastCompletedSetForUserAndExercise(
                        user.getId(),
                        catalogExerciseId,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);



        WorkoutExerciseDetailResponse response =
                WorkoutExerciseViewMapper.toDetailResponse(session, exercise, previousSet);

        return ResponseEntity.ok(response);
    }

    // SALTAR SESIÓN (la marca como CANCELLED y avanza el progreso de rutina)
    @PostMapping("/skip")
    public ResponseEntity<SkipWorkoutResponse> skipWorkout(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid StartWorkoutRequest request
    ) {
        WorkoutSession session = workoutSessionService.skipSession(
                user.getId(),
                request.routineDetailId()
        );

        return ResponseEntity.ok(new SkipWorkoutResponse(session.getId(), "Sesión saltada"));
    }



    @PatchMapping("/sessions/{sessionId}/exercises/reorder-next")
    public ResponseEntity<Void> reorderNextExercises(
            @PathVariable Long sessionId,
            @RequestBody ReorderNextExercisesRequest request,
            @AuthenticationPrincipal User user   // ⬅️ aquí va TU entidad User, no CustomUserDetails
    ) {
        workoutSessionService.reorderNextExercises(
                sessionId,
                user.getId(),                     // ⬅️ esto ahora compila, User sí tiene getId()
                request.workoutExerciseIds()
        );
        return ResponseEntity.noContent().build();
    }

    // 3) OBTENER RESUMEN DE SESIÓN COMPLETADA
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<WorkoutSessionSummaryResponse> getSessionSummary(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user
    ) {
        WorkoutSessionSummaryResponse summary = workoutSessionService.getSessionSummary(
                sessionId,
                user.getId()
        );
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{sessionId}/detail")
    public ResponseEntity<WorkoutSessionDetailResponse> getSessionDetail(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user
    ) {
        var detail = workoutSessionService.getSessionDetail(sessionId, user.getId());
        return ResponseEntity.ok(detail);
    }

    // DESCARTAR SESIÓN (elimina todo el progreso y marca como CANCELLED)
    @PostMapping("/{sessionId}/discard")
    public ResponseEntity<Void> discardSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user
    ) {
        workoutSessionService.discardSession(sessionId, user.getId());
        return ResponseEntity.noContent().build();
    }

    // FINALIZAR SESIÓN (guarda el progreso actual y marca como COMPLETED)
    @PostMapping("/{sessionId}/finish")
    public ResponseEntity<Void> finishSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user
    ) {
        workoutSessionService.finishSession(sessionId, user.getId());
        return ResponseEntity.noContent().build();
    }
    private Long resolveCatalogExerciseId(WorkoutExercise we) {

        if (we.getRoutineExercise() != null && we.getRoutineExercise().getExercise() != null) {
            return we.getRoutineExercise().getExercise().getId();
        }

        if (we.getExercise() != null) {
            return we.getExercise().getId();
        }

        throw new NotFoundException("No se pudo determinar el ejercicio de catálogo para previous set.");
    }


}
