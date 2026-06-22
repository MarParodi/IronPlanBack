package com.example.ironplan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnapshotScheduler {

    private final SnapshotService snapshotService;

    /** Cada domingo 23:55 */
    @Scheduled(cron = "0 55 23 * * SUN")
    public void generarSnapshotSemanal() {
        snapshotService.procesarRetosActivos();
    }

    /** Mortalidad experimental: lunes 08:00 */
    @Scheduled(cron = "0 0 8 * * MON")
    public void aplicarMortalidad() {
        snapshotService.aplicarMortalidadExperimental();
    }
}
