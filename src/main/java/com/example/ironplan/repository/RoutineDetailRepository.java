// src/main/java/com/example/ironplan/repository/RoutineDetailRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.RoutineDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutineDetailRepository extends JpaRepository<RoutineDetail, Long> {

    // Para obtener una sesión verificando que pertenece a esa rutina
    Optional<RoutineDetail> findByIdAndRoutine_Id(Long sessionId, Long routineId);

    // Listar todas las sesiones de una rutina ordenadas por bloque y orden
    List<RoutineDetail> findByRoutine_IdOrderByBlockNumberAscOrderInBlockAsc(Long routineId);

    // Listar sesiones de un bloque específico de una rutina
    List<RoutineDetail> findByRoutine_IdAndBlockNumberOrderByOrderInBlockAsc(Long routineId, Integer blockNumber);
}
