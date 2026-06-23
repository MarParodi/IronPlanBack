package com.example.ironplan.repository;

import com.example.ironplan.model.ExperimentoReto;
import com.example.ironplan.model.ExperimentoRetoEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentoRetoRepository extends JpaRepository<ExperimentoReto, Long> {

    List<ExperimentoReto> findByEstado(ExperimentoRetoEstado estado);

    java.util.Optional<ExperimentoReto> findByCompetitionId(Long competitionId);
}
