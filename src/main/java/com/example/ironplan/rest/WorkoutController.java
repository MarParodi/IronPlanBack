package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.model.WorkoutExercise;
import com.example.ironplan.model.WorkoutSession;
import com.example.ironplan.model.WorkoutSet;
import com.example.ironplan.repository.WorkoutExerciseRepository;
import com.example.ironplan.repository.WorkoutSetRepository;
import com.example.ironplan.rest.dto.ReorderNextExercisesRequest;
import com.example.ironplan.rest.dto.StartWorkoutRequest;
import com.example.ironplan.rest.dto.WorkoutExerciseDetailResponse;
import com.example.ironplan.rest.dto.WorkoutSessionSummaryResponse;
import com.example.ironplan.rest.mapper.WorkoutExerciseViewMapper;
import com.example.ironplan.rest.error.NotFoundException;
import com.example.ironplan.service.WorkoutExerciseService;
import com.example.ironplan.service.WorkoutSessionService;
import jakarta.validation.Valid;
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

    // 1) INICIAR SESIÓN DE ENTRENAMIENTO
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

        // última serie completada (no habrá al inicio, pero dejamos la lógica lista)
        WorkoutSet previousSet = workoutSetRepo
                .findFirstByWorkoutExercise_IdAndCompletedIsTrueOrderBySetNumberDesc(
                        firstExercise.getId()
                )
                .orElse(null);

        WorkoutExerciseDetailResponse response =
                WorkoutExerciseViewMapper.toDetailResponse(session, firstExercise, previousSet);

        return ResponseEntity.ok(response);
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
        WorkoutSet previousSet = workoutSetRepo
                .findFirstByWorkoutExercise_IdAndCompletedIsTrueOrderBySetNumberDesc(
                        exercise.getId()
                )
                .orElse(null);

        WorkoutExerciseDetailResponse response =
                WorkoutExerciseViewMapper.toDetailResponse(session, exercise, previousSet);

        return ResponseEntity.ok(response);
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

}
