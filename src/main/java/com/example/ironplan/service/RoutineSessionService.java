// src/main/java/com/example/ironplan/service/RoutineSessionService.java
package com.example.ironplan.service;

import com.example.ironplan.repository.RoutineDetailRepository;
import com.example.ironplan.rest.dto.RoutineSessionDetailDto;
import com.example.ironplan.rest.error.NotFoundException;
import com.example.ironplan.rest.mapper.RoutineSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineSessionService {

    private final RoutineDetailRepository detailRepo;

    public RoutineSessionService(RoutineDetailRepository detailRepo) {
        this.detailRepo = detailRepo;
    }

    @Transactional(readOnly = true)
    public RoutineSessionDetailDto getSessionDetail(Long routineId, Long sessionId) {
        var detail = detailRepo.findByIdAndRoutine_Id(sessionId, routineId)
                .orElseThrow(() -> new NotFoundException(
                        "Sesión " + sessionId + " no encontrada para la rutina " + routineId
                ));

        return RoutineSessionMapper.toDetailDto(detail);
    }
}
