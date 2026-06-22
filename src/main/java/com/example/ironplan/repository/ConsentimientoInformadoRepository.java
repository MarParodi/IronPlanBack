package com.example.ironplan.repository;

import com.example.ironplan.model.ConsentimientoInformado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentimientoInformadoRepository extends JpaRepository<ConsentimientoInformado, Long> {

    Optional<ConsentimientoInformado> findByParticipanteRetoId(Long participanteRetoId);
}
