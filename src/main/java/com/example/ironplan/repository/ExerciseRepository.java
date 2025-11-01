package com.example.ironplan.repository;

import com.example.ironplan.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    // Búsqueda SOLO por nombre (case-insensitive)
    Page<Exercise> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Autocomplete (los primeros 10 que empiecen con el prefijo)
    List<Exercise> findTop10ByNameStartingWithIgnoreCase(String prefix);

    // Búsqueda “amplia” (nombre o músculos)
    @Query("""
           SELECT e FROM Exercise e
           WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(e.primaryMuscle) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(e.secondaryMuscle) LIKE LOWER(CONCAT('%', :q, '%'))
           """)
    Page<Exercise> searchWide(String q, Pageable pageable);

}
