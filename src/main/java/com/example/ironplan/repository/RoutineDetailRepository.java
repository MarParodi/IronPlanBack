// src/main/java/com/example/ironplan/repository/RoutineDetailRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.RoutineDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutineDetailRepository extends JpaRepository<RoutineDetail, Long> {

    // Para obtener una sesión verificando que pertenece a ese bloque
    Optional<RoutineDetail> findByIdAndBlock_Id(Long sessionId, Long blockId);

    // Listar todas las sesiones de un bloque ordenadas por sessionOrder
    List<RoutineDetail> findByBlock_IdOrderBySessionOrderAsc(Long blockId);

    // Listar todas las sesiones de una rutina (a través del bloque)
    List<RoutineDetail> findByBlock_Routine_IdOrderByBlock_OrderIndexAscSessionOrderAsc(Long routineId);
}
