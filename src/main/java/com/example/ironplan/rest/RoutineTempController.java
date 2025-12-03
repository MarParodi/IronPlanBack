// src/main/java/com/example/ironplan/rest/RoutineTemplateController.java
package com.example.ironplan.rest;

import com.example.ironplan.model.Goal;
import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.CreateRoutineRequest;
import com.example.ironplan.rest.dto.CreateRoutineResponse;
import com.example.ironplan.rest.dto.RoutineDetailResponse;
import com.example.ironplan.rest.dto.RoutineListItemResponse;
import com.example.ironplan.rest.dto.routine.RoutineOverviewResponse;
import com.example.ironplan.service.RoutineTemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routines")
public class RoutineTempController {

    private final RoutineTemplateService service;

    public RoutineTempController(RoutineTemplateService service) {
        this.service = service;
    }

    /**
     * Crear una nueva rutina
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRoutineResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRoutineRequest request
    ) {
        return service.create(user, request);
    }

    @GetMapping("/{id}")
    public RoutineDetailResponse getById(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @GetMapping
    public Page<RoutineListItemResponse> listPublic(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Goal goal,
            @RequestParam(required = false) Integer daysPerWeek,
            Pageable pageable
    ) {
        return service.listPublic(search, goal, daysPerWeek, pageable);
    }

    @GetMapping("/{id}/overview")
    public RoutineOverviewResponse getOverview(@PathVariable Long id) {
        return service.getOverview(id);
    }

    // (Opcional) Admin: todas las rutinas (restringir con rol ADMIN si lo usas)
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public Page<RoutineListItemResponse> listAll(Pageable pageable) {
        return service.listAll(pageable);
    }
}
