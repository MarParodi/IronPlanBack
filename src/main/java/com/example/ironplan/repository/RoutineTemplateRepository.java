// src/main/java/com/example/ironplan/repository/RoutineTemplateRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.Goal;
import com.example.ironplan.model.RoutineGender;
import com.example.ironplan.model.RoutineStatus;
import com.example.ironplan.model.RoutineTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RoutineTemplateRepository extends JpaRepository<RoutineTemplate, Long> {

    Optional<RoutineTemplate> findById(Long id);

    // Catálogo público básico (PUBLISHED + isPublic=true)
    Page<RoutineTemplate> findByStatusAndIsPublicTrue(RoutineStatus status, Pageable pageable);

    // Catálogo público con filtros opcionales de búsqueda, goal y días por semana
    @Query("""
           SELECT r FROM RoutineTemplate r
           WHERE r.status = :status AND r.isPublic = true
           AND (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))
           AND (:goal IS NULL OR r.goal = :goal)
           AND (:daysPerWeek IS NULL OR r.days_per_week = :daysPerWeek) AND (:routineGender IS NULL OR r.routineGender = :routineGender)
           """)
    Page<RoutineTemplate> findPublicWithFilters(
            @Param("status") RoutineStatus status,
            @Param("search") String search,
            @Param("goal") Goal goal,
            @Param("daysPerWeek") Integer daysPerWeek,
            @Param("routineGender") RoutineGender routineGender,
            Pageable pageable
    );

    long countByUser_Id(Long userId);

    // (Opcional) Todas las rutinas (para panel admin)
    Page<RoutineTemplate> findAll(Pageable pageable);

    Page<RoutineTemplate> findByUser_Id(Long userId, Pageable pageable);

    Page<RoutineTemplate> findByUser_IdAndStatusNot(
            Long userId,
            RoutineStatus status,
            Pageable pageable
    );

}
