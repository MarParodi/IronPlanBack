// src/main/java/com/example/ironplan/rest/RoutineTemplateController.java
package com.example.ironplan.rest;

import com.example.ironplan.model.Goal;
import com.example.ironplan.model.RoutineGender;
import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.CreateRoutineRequest;
import com.example.ironplan.rest.dto.CreateRoutineResponse;
import com.example.ironplan.rest.dto.RoutineDetailResponse;
import com.example.ironplan.rest.dto.RoutineListItemResponse;
import com.example.ironplan.rest.dto.routine.RoutineOverviewResponse;
import com.example.ironplan.service.CloudinaryService;
import com.example.ironplan.service.RoutineTemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/routines")
public class RoutineTempController {

    private final RoutineTemplateService service;
    private final CloudinaryService cloudinaryService;

    public RoutineTempController(RoutineTemplateService service, CloudinaryService cloudinaryService) {
        this.service = service;
        this.cloudinaryService = cloudinaryService;
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

    @GetMapping("/mine")
    public Page<RoutineListItemResponse> getMyRoutines(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        return service.listMine(user, pageable);
    }

    @PostMapping("/upload-image")
    public Map<String, Object> uploadRoutineImage(@RequestParam("file") MultipartFile file) throws IOException {
        Map uploadResult = cloudinaryService.upload(file, "routines");
        // Cloudinary normalmente regresa "secure_url"
        return Map.of(
                "url", uploadResult.get("secure_url"),
                "publicId", uploadResult.get("public_id")
        );
    }

//desbloquear rutina con xp
@PostMapping("/{id}/unlock")
public ResponseEntity<?> unlockRoutine(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
) {
    service.unlockWithXp(user, id);
    return ResponseEntity.ok().build();
}


    @GetMapping("/{id}")
    public RoutineDetailResponse getById(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @GetMapping
    public Page<RoutineListItemResponse> listPublic(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Goal goal,
            @RequestParam(required = false) Integer daysPerWeek,
            @RequestParam(required = false) RoutineGender routineGender,
            Pageable pageable
    ) {
        return service.listPublic(user, search, goal, daysPerWeek, routineGender, pageable);
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
