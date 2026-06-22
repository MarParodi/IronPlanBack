package com.example.ironplan.repository;

import com.example.ironplan.model.SusRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SusRespuestaRepository extends JpaRepository<SusRespuesta, Long> {

    Optional<SusRespuesta> findByParticipanteRetoId(Long participanteRetoId);

    @Query("""
        SELECT s FROM SusRespuesta s
        JOIN s.participanteReto p
        WHERE p.reto.id = :retoId
        """)
    List<SusRespuesta> findByRetoId(@Param("retoId") Long retoId);

    @Query("""
        SELECT AVG(s.puntajeSus) FROM SusRespuesta s
        JOIN s.participanteReto p
        WHERE p.reto.id = :retoId
        """)
    BigDecimal avgPuntajeByRetoId(@Param("retoId") Long retoId);
}
