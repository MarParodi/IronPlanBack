package com.example.ironplan.service;

import org.springframework.stereotype.Service;
import com.example.ironplan.model.Exercise;
import com.example.ironplan.repository.ExerciseRepository;
import com.example.ironplan.rest.dto.ExerciseCreateReq;
import com.example.ironplan.rest.dto.ExerciseUpdateReq;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class ExerciseService {
    private final ExerciseRepository repo;

    public ExerciseService(ExerciseRepository repo) {
        this.repo = repo;
    }

    // LISTAR (paginado)
    public Page<Exercise> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    // OBTENER por ID
    public Exercise getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exercise id=" + id + " no encontrado"));
    }
    public Page<Exercise> searchByName(String q, Pageable pageable) {
        return repo.findByNameContainingIgnoreCase(q.trim(), pageable);
    }

    public Page<Exercise> searchWide(String q, Pageable pageable) {
        return repo.searchWide(q.trim(), pageable);
    }

    public List<String> suggestNames(String prefix) {
        return repo.findTop10ByNameStartingWithIgnoreCase(prefix.trim())
                .stream()
                .map(Exercise::getName)
                .toList();
    }

    // CREAR
    @Transactional
    public Exercise create(@Valid ExerciseCreateReq req) {
        // Regla de unicidad por nombre (opcional pero recomendado)
        if (repo.existsByNameIgnoreCase(req.name())) {
            throw new IllegalArgumentException("Ya existe un ejercicio con ese nombre.");
        }

        Exercise e = new Exercise();
        e.setName(req.name().trim());
        e.setDescription(req.description().trim());
        e.setInstructions(req.instructions().trim());
        e.setPrimaryMuscle(req.primaryMuscle().trim());
        e.setSecondaryMuscle(req.secondaryMuscle().trim());
        e.setVideoUrl(req.videoUrl().trim());

        return repo.save(e);
    }

    // ACTUALIZAR (PUT)
    @Transactional
    public Exercise update(Long id, @Valid ExerciseUpdateReq req) {
        Exercise e = getById(id);

        // Evita duplicar nombre en otro registro
        repo.findByNameIgnoreCase(req.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new IllegalArgumentException("Ya existe un ejercicio con ese nombre."); });

        e.setName(req.name().trim());
        e.setDescription(req.description().trim());
        e.setInstructions(req.instructions().trim());
        e.setPrimaryMuscle(req.primaryMuscle().trim());
        e.setSecondaryMuscle(req.secondaryMuscle().trim());
        e.setVideoUrl(req.videoUrl().trim());
        return repo.save(e);
    }

    // ELIMINAR
    @Transactional
    public void delete(Long id) {
        Exercise e = getById(id); // lanza 404 si no existe
        repo.delete(e);
    }
}
