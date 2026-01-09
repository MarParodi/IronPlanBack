// src/main/java/com/example/ironplan/service/RoutineSessionService.java
package com.example.ironplan.service;

import com.example.ironplan.repository.RoutineDetailRepository;
import com.example.ironplan.repository.RoutineBlockRepository;
import com.example.ironplan.rest.dto.RoutineSessionDetailDto;
import com.example.ironplan.rest.error.NotFoundException;
import com.example.ironplan.rest.mapper.RoutineSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineSessionService {

    private final RoutineDetailRepository detailRepo;
    private final RoutineBlockRepository blockRepo;

    public RoutineSessionService(RoutineDetailRepository detailRepo,
                                  RoutineBlockRepository blockRepo) {
        this.detailRepo = detailRepo;
        this.blockRepo = blockRepo;
    }

    @Transactional(readOnly = true)
    public RoutineSessionDetailDto getSessionDetail(Long blockId, Long sessionId) {
        var detail = detailRepo.findByIdAndBlock_Id(sessionId, blockId)
                .orElseThrow(() -> new NotFoundException(
                        "Sesión " + sessionId + " no encontrada para el bloque " + blockId
                ));

        return RoutineSessionMapper.toDetailDto(detail);
    }

    @Transactional(readOnly = true)
    public RoutineSessionDetailDto getSessionDetailByRoutine(Long routineId, Long sessionId) {
        // Buscar la sesión verificando que pertenece a la rutina (a través del bloque)
        var sessions = detailRepo.findByBlock_Routine_IdOrderByBlock_OrderIndexAscSessionOrderAsc(routineId);
        var detail = sessions.stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Sesión " + sessionId + " no encontrada para la rutina " + routineId
                ));

        return RoutineSessionMapper.toDetailDto(detail);
    }
}
