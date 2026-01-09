package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.*;
import com.example.ironplan.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal User user
    ) {
        ProfileResponse response = profileService.getProfile(user);
        return ResponseEntity.ok(response);
    }

    // -------- RUTINA ACTUAL --------

    /**
     * Obtener la rutina actual del usuario
     */
    @GetMapping("/my-routine")
    public ResponseEntity<CurrentRoutineResponse> getMyCurrentRoutine(
            @AuthenticationPrincipal User user
    ) {
        CurrentRoutineResponse response = profileService.getCurrentRoutine(user);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Empezar/asignar una rutina al usuario
     */
    @PostMapping("/start-routine")
    public ResponseEntity<Void> startRoutine(
            @AuthenticationPrincipal User user,
            @RequestBody StartRoutineRequest request
    ) {
        profileService.startRoutine(user, request.routineId());
        return ResponseEntity.ok().build();
    }

    /**
     * Dejar/quitar la rutina actual
     */
    @PostMapping("/stop-routine")
    public ResponseEntity<Void> stopRoutine(
            @AuthenticationPrincipal User user
    ) {
        profileService.stopRoutine(user);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtener la rutina activa con progreso completo (para pantalla Mi Rutina)
     */
    @GetMapping("/my-routine/full")
    public ResponseEntity<ActiveRoutineResponse> getMyRoutineWithProgress(
            @AuthenticationPrincipal User user
    ) {
        ActiveRoutineResponse response = profileService.getActiveRoutineWithProgress(user);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Reordenar las sesiones dentro de un bloque
     */
    @PostMapping("/my-routine/reorder")
    public ResponseEntity<Void> reorderSessions(
            @AuthenticationPrincipal User user,
            @RequestBody ReorderSessionsRequest request
    ) {
        profileService.reorderSessions(
                user,
                request.routineId(),
                request.blockId(),
                request.sessionIds()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/workouts")
    public ResponseEntity<List<RecentWorkoutDto>> getWorkoutHistory(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(profileService.getWorkoutHistory(currentUser));
    }

}
