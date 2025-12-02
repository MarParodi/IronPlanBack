// src/main/java/com/example/ironplan/rest/RoutineSessionController.java
package com.example.ironplan.rest;

import com.example.ironplan.rest.dto.RoutineSessionDetailDto;
import com.example.ironplan.service.RoutineSessionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routines")
public class RoutineSessionController {

    private final RoutineSessionService sessionService;

    public RoutineSessionController(RoutineSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/{routineId}/sessions/{sessionId}")
    public RoutineSessionDetailDto getSessionDetail(
            @PathVariable Long routineId,
            @PathVariable Long sessionId
    ) {
        return sessionService.getSessionDetail(routineId, sessionId);
    }
}
