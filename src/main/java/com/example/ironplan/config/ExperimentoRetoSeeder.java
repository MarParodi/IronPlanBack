package com.example.ironplan.config;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.ExperimentoRetoRepository;
import com.example.ironplan.repository.OrganizationalGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Crea el reto experimental "Pongámonos en Forma 2026" si no existe (perfil dev).
 */
@Component
@Profile("dev")
public class ExperimentoRetoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExperimentoRetoSeeder.class);

    private final ExperimentoRetoRepository retoRepo;
    private final OrganizationalGroupRepository groupRepo;

    public ExperimentoRetoSeeder(
            ExperimentoRetoRepository retoRepo,
            OrganizationalGroupRepository groupRepo) {
        this.retoRepo = retoRepo;
        this.groupRepo = groupRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (retoRepo.findAll().stream().anyMatch(r -> r.getNombre().contains("Pongámonos en Forma"))) {
            return;
        }
        var org = groupRepo.findAll().stream()
                .filter(g -> g.getGroupType() == GroupType.EMPRESA)
                .findFirst()
                .orElse(null);
        if (org == null) {
            log.warn("ExperimentoRetoSeeder: no hay org EMPRESA, omitiendo seed");
            return;
        }

        ExperimentoReto reto = ExperimentoReto.builder()
                .nombre("Pongámonos en Forma 2026")
                .descripcion("Reto HTI · GTS · CTN — experimento de tesis UAS")
                .organizacion(org)
                .fechaInicio(LocalDate.of(2026, 7, 1))
                .fechaFin(LocalDate.of(2026, 9, 1))
                .semanasIntervencion(8)
                .estado(ExperimentoRetoEstado.PLANEACION)
                .ambito(ExperimentoAmbito.ORGANIZACION)
                .build();
        retoRepo.save(reto);
        log.info("ExperimentoRetoSeeder: creado reto id={} '{}'", reto.getId(), reto.getNombre());
    }
}
