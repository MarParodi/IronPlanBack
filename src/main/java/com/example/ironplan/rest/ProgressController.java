package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.progress.*;
import com.example.ironplan.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * GET /api/progress/summary
     * Obtiene resumen general de progreso del usuario
     */
    @GetMapping("/summary")
    public ResponseEntity<ProgressSummaryDto> getProgressSummary(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "8") int weeks
    ) {
        ProgressSummaryDto summary = progressService.getProgressSummary(user, weeks);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/progress/weekly
     * Obtiene historial semanal
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklyStatsDto>> getWeeklyStats(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "8") int weeks
    ) {
        List<WeeklyStatsDto> weekly = progressService.getWeeklyHistory(user, weeks);
        return ResponseEntity.ok(weekly);
    }

    /**
     * GET /api/progress/exercises/{exerciseId}
     * Obtiene historial de progreso de un ejercicio específico
     */
    @GetMapping("/exercises/{exerciseId}")
    public ResponseEntity<ExerciseProgressDto> getExerciseProgress(
            @AuthenticationPrincipal User user,
            @PathVariable Long exerciseId,
            @RequestParam(defaultValue = "10") int sessions
    ) {
        ExerciseProgressDto progress = progressService.getExerciseProgress(user, exerciseId, sessions);
        return ResponseEntity.ok(progress);
    }

    /**
     * GET /api/progress/exercises/{exerciseId}/recommendation
     * Obtiene recomendación de progresión para un ejercicio
     */
    @GetMapping("/exercises/{exerciseId}/recommendation")
    public ResponseEntity<ProgressionRecommendationDto> getProgressionRecommendation(
            @AuthenticationPrincipal User user,
            @PathVariable Long exerciseId,
            @RequestParam int plannedSets,
            @RequestParam int repsMin,
            @RequestParam int repsMax
    ) {
        ProgressionRecommendationDto recommendation = progressService.getProgressionRecommendation(
                user, exerciseId, plannedSets, repsMin, repsMax
        );
        return ResponseEntity.ok(recommendation);
    }

    /**
     * POST /api/progress/calculate-1rm
     * Calcula 1RM estimado (útil para el frontend)
     */
    @PostMapping("/calculate-1rm")
    public ResponseEntity<Calculate1RMResponse> calculate1RM(@RequestBody Calculate1RMRequest request) {
        Double estimated1RM = ProgressService.calculate1RM(request.weightKg(), request.reps());
        return ResponseEntity.ok(new Calculate1RMResponse(
                request.weightKg(),
                request.reps(),
                estimated1RM
        ));
    }

    // DTOs internos para 1RM
    public record Calculate1RMRequest(double weightKg, int reps) {}
    public record Calculate1RMResponse(double weightKg, int reps, Double estimated1RM) {}
}
