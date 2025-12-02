// src/main/java/com/example/ironplan/rest/RoutineTemplateController.java
package com.example.ironplan.rest;

import com.example.ironplan.rest.dto.RoutineDetailResponse;
import com.example.ironplan.rest.dto.RoutineListItemResponse;
import com.example.ironplan.rest.dto.routine.RoutineOverviewResponse;
import com.example.ironplan.service.RoutineTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routines")
public class RoutineTempController {

    private final RoutineTemplateService service;

    public RoutineTempController(RoutineTemplateService service) {
        this.service = service;
    }
    @GetMapping("/{id}")
    public RoutineDetailResponse getById(@PathVariable Long id) {
        return service.getDetail(id);
    }
    @GetMapping
    public Page<RoutineListItemResponse> listPublic(Pageable pageable) {
        return service.listPublic(pageable);
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
