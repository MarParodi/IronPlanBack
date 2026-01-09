// src/main/java/com/example/ironplan/repository/RoutineBlockRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.RoutineBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutineBlockRepository extends JpaRepository<RoutineBlock, Long> {

    // Listar todos los bloques de una rutina ordenados por orderIndex
    List<RoutineBlock> findByRoutine_IdOrderByOrderIndexAsc(Long routineId);

    // Para obtener un bloque verificando que pertenece a esa rutina
    Optional<RoutineBlock> findByIdAndRoutine_Id(Long blockId, Long routineId);

    // Contar bloques de una rutina
    long countByRoutine_Id(Long routineId);
}

