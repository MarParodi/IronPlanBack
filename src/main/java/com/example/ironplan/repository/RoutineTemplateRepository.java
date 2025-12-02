// src/main/java/com/example/ironplan/repository/RoutineTemplateRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.RoutineStatus;
import com.example.ironplan.model.RoutineTemplate;
import com.example.ironplan.rest.dto.RoutineListItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RoutineTemplateRepository extends JpaRepository<RoutineTemplate, Long> {

    Optional<RoutineTemplate> findById(Long id);

    // Catálogo público (PUBLISHED + isPublic=true)
    @Query("""
           select new com.example.ironplan.rest.dto.RoutineListItemResponse(
               r.id, r.name, r.goal, r.access, r.img, r.description
           )
           from RoutineTemplate r
           where r.status = :status and r.isPublic = true
           """)
    Page<RoutineListItemResponse> findPublicList(RoutineStatus status, Pageable pageable);
    long countByUser_Id(Long userId);
    // (Opcional) Todas las rutinas (para panel admin)
    @Query("""
           select new com.example.ironplan.rest.dto.RoutineListItemResponse(
               r.id, r.name, r.goal, r.access, r.img, r.description
           )
           from RoutineTemplate r
           """)
    Page<RoutineListItemResponse> findAllList(Pageable pageable);
}
