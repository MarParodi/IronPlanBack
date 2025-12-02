// src/main/java/com/example/ironplan/service/RoutineTemplateService.java
package com.example.ironplan.service;

import com.example.ironplan.model.RoutineStatus;
import com.example.ironplan.model.RoutineTemplate;
import com.example.ironplan.model.RoutineDetail;
import com.example.ironplan.repository.RoutineTemplateRepository;
import com.example.ironplan.repository.RoutineDetailRepository;
import com.example.ironplan.rest.dto.RoutineDetailResponse;
import com.example.ironplan.rest.dto.RoutineListItemResponse;
import com.example.ironplan.rest.dto.routine.RoutineOverviewResponse;
import com.example.ironplan.rest.dto.routine.RoutineBlockDto;
import com.example.ironplan.rest.dto.routine.RoutineBlockItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoutineTemplateService {

    private final RoutineTemplateRepository repo;
    private final RoutineDetailRepository detailRepo;   // 👈 NUEVO

    // ⬇️ Actualiza el constructor para inyectar ambos repos
    public RoutineTemplateService(RoutineTemplateRepository repo,
                                  RoutineDetailRepository detailRepo) {
        this.repo = repo;
        this.detailRepo = detailRepo;
    }

    // ---------- Detalle “ligero” (para modal) ----------
    public RoutineTemplate getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Routine not found"));
    }

    public RoutineDetailResponse getDetail(Long id) {
        RoutineTemplate r = getById(id);

        return new RoutineDetailResponse(
                r.getId(),
                r.getName(),
                r.getGoal(),           // según tu DTO: String / Enum
                r.getAccess(),         // idem
                r.getDescription(),
                r.getSuggestedLevel()  // nivel sugerido para el modal
        );
    }

    // ---------- Listas ----------
    // Catálogo público publicado
    public Page<RoutineListItemResponse> listPublic(Pageable pageable) {
        return repo.findPublicList(RoutineStatus.PUBLISHED, pageable);
    }

    // Panel admin (todas)
    public Page<RoutineListItemResponse> listAll(Pageable pageable) {
        return repo.findAllList(pageable);
    }

    // ---------- OVERVIEW grande (Rutina Híbrida) ----------
    public RoutineOverviewResponse getOverview(Long routineId) {
        RoutineTemplate r = getById(routineId);

        // Traer todas las sesiones (RoutineDetail) ordenadas
        List<RoutineDetail> details = detailRepo
                .findByRoutine_IdOrderByBlockNumberAscOrderInBlockAsc(routineId);

        // Agrupar por bloque
        Map<Integer, List<RoutineDetail>> grouped =
                details.stream().collect(Collectors.groupingBy(
                        RoutineDetail::getBlockNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RoutineBlockDto> blocks = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            int blockNumber = entry.getKey();
            List<RoutineDetail> sessions = entry.getValue();

            String blockLabel = sessions.get(0).getBlockLabel(); // Semana 1–12, etc.

            List<RoutineBlockItemDto> items = sessions.stream()
                    .map(d -> new RoutineBlockItemDto(
                            d.getId(),
                            d.getTitle(),
                            d.getTotalSeries(),
                            d.getMuscles()
                    ))
                    .toList();

            blocks.add(new RoutineBlockDto(
                    blockNumber,
                    "Bloque " + blockNumber + " — " + blockLabel,
                    items
            ));
        }

        return new RoutineOverviewResponse(
                r.getId(),
                r.getName(),
                r.getDurationWeeks(),
                r.getLongDescription(),
                r.getGoal() != null ? r.getGoal().name() : null,
                r.getSuggestedLevel() != null ? r.getSuggestedLevel().name() : null,
                r.getDays_per_week(),
                blocks
        );
    }
}

