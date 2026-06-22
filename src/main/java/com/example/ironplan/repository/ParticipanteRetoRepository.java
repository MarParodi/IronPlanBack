package com.example.ironplan.repository;

import com.example.ironplan.model.ParticipanteReto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipanteRetoRepository extends JpaRepository<ParticipanteReto, Long> {

    Optional<ParticipanteReto> findByRetoIdAndUsuarioId(Long retoId, Long usuarioId);

    List<ParticipanteReto> findByRetoId(Long retoId);

    List<ParticipanteReto> findByRetoIdAndActivoTrue(Long retoId);

    long countByRetoId(Long retoId);

    long countByRetoIdAndCompletoPretestTrue(Long retoId);

    long countByRetoIdAndCompletoPosttestTrue(Long retoId);

    long countByRetoIdAndCompletoSusTrue(Long retoId);

    long countByRetoIdAndActivoTrue(Long retoId);

    @Query("""
        SELECT p FROM ParticipanteReto p
        JOIN FETCH p.usuario
        WHERE p.reto.id = :retoId
        """)
    List<ParticipanteReto> findByRetoIdWithUsuario(@Param("retoId") Long retoId);
}
