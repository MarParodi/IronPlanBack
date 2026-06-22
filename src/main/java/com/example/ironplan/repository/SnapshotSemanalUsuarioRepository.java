package com.example.ironplan.repository;

import com.example.ironplan.model.SnapshotSemanalUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SnapshotSemanalUsuarioRepository extends JpaRepository<SnapshotSemanalUsuario, Long> {

    List<SnapshotSemanalUsuario> findByRetoIdAndUsuarioIdOrderByNumeroSemanaAsc(Long retoId, Long usuarioId);

    Optional<SnapshotSemanalUsuario> findByRetoIdAndUsuarioIdAndNumeroSemana(
            Long retoId, Long usuarioId, Integer numeroSemana);

    List<SnapshotSemanalUsuario> findByRetoId(Long retoId);
}
