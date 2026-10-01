package com.example.ironplan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnapshotScheduler {

    private final SnapshotService snapshotService;

    /** Lunes 00:30: recalcula todas las semanas, incluida la que acaba de cerrar. */
    @Scheduled(cron = "0 30 0 * * MON")
    public void generarSnapshotSemanal() {
        snapshotService.procesarRetosActivos();
    }

    /** Mortalidad experimental: lunes 08:00 */
    @Scheduled(cron = "0 0 8 * * MON")
    public void aplicarMortalidad() {
        snapshotService.aplicarMortalidadExperimental();
    }
}
