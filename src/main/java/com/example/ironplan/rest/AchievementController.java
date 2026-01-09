package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.AchievementDto;
import com.example.ironplan.rest.dto.UserAchievementDto;
import com.example.ironplan.service.AchievementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    /**
     * Obtiene todas las hazañas con estado de desbloqueo para el usuario actual
     */
    @GetMapping
    public ResponseEntity<List<AchievementDto>> getAllAchievements(
            @AuthenticationPrincipal User user
    ) {
        List<AchievementDto> achievements = achievementService.getAllAchievementsForUser(user);
        return ResponseEntity.ok(achievements);
    }

    /**
     * Obtiene estadísticas de hazañas del usuario
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAchievementStats(
            @AuthenticationPrincipal User user
    ) {
        Map<String, Object> stats = achievementService.getAchievementStats(user);
        return ResponseEntity.ok(stats);
    }

    /**
     * Obtiene hazañas recién desbloqueadas (no vistas)
     */
    @GetMapping("/unseen")
    public ResponseEntity<List<UserAchievementDto>> getUnseenAchievements(
            @AuthenticationPrincipal User user
    ) {
        List<UserAchievementDto> unseen = achievementService.getUnseenAchievements(user);
        return ResponseEntity.ok(unseen);
    }

    /**
     * Marca hazañas como vistas
     */
    @PostMapping("/mark-seen")
    public ResponseEntity<Void> markAsSeen(
            @AuthenticationPrincipal User user,
            @RequestBody MarkSeenRequest request
    ) {
        achievementService.markAchievementsAsSeen(user, request.codes());
        return ResponseEntity.ok().build();
    }

    public record MarkSeenRequest(List<String> codes) {}
}
