package com.example.ironplan.repository;

import com.example.ironplan.model.IpaqCorte;
import com.example.ironplan.model.IpaqRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpaqRespuestaRepository extends JpaRepository<IpaqRespuesta, Long> {

    Optional<IpaqRespuesta> findByParticipanteRetoIdAndCorte(Long participanteRetoId, IpaqCorte corte);
}
