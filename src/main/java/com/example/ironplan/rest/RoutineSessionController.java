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

    // Endpoint para obtener detalle de sesión por rutina (busca a través de los bloques)
    @GetMapping("/{routineId}/sessions/{sessionId}")
    public RoutineSessionDetailDto getSessionDetail(
            @PathVariable Long routineId,
            @PathVariable Long sessionId
    ) {
        return sessionService.getSessionDetailByRoutine(routineId, sessionId);
    }

    // Nuevo endpoint para obtener detalle de sesión por bloque
    @GetMapping("/blocks/{blockId}/sessions/{sessionId}")
    public RoutineSessionDetailDto getSessionDetailByBlock(
            @PathVariable Long blockId,
            @PathVariable Long sessionId
    ) {
        return sessionService.getSessionDetail(blockId, sessionId);
    }
}
