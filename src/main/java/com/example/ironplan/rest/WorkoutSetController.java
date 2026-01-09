package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.WorkoutPreviousSetDto;
import com.example.ironplan.rest.dto.WorkoutSetItemRequest;
import com.example.ironplan.rest.dto.WorkoutSetRequest;
import com.example.ironplan.service.WorkoutSetService;
import com.example.ironplan.rest.dto.WorkoutSetInput;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutSetController {

    private final WorkoutSetService workoutSetService;

    public WorkoutSetController(WorkoutSetService workoutSetService) {
        this.workoutSetService = workoutSetService;
    }

    @PostMapping("/{sessionId}/exercises/{exerciseId}/sets")
    public ResponseEntity<Void> saveSetsForExercise(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @AuthenticationPrincipal User user,
            @RequestBody @Valid WorkoutSetRequest request
    ) {
        // Mapear los DTOs del REST a los DTOs internos del servicio
        List<WorkoutSetInput> inputs = request.sets().stream()
                .map(WorkoutSetController::toInput)
                .toList();

        workoutSetService.saveSetsForExercise(
                sessionId,
                exerciseId,
                user.getId(),
                inputs,
                request.notes()
        );

        // 204 No Content: operación OK sin body
        return ResponseEntity.noContent().build();
    }




    private static WorkoutSetInput toInput(WorkoutSetItemRequest item) {
        return new WorkoutSetInput(
                item.setNumber(),
                item.reps(),
                item.weightKg(),
                Boolean.TRUE.equals(item.completed())
        );
    }
}
