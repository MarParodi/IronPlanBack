package com.example.ironplan.service;

import com.example.ironplan.config.RetoPointsProperties;
import com.example.ironplan.model.FreeActivityType;
import com.example.ironplan.model.RetoActivityReviewFlag;
import com.example.ironplan.model.RetoActivitySource;
import com.example.ironplan.repository.projection.ActividadLibreDetalle;
import com.example.ironplan.repository.projection.SesionFuerzaDetalle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoAdminActivityServiceTest {

    private static final Long ANA = 1L;
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 2);

    private RetoPointsProperties props;
    private RetoAdminActivityService service;
    private final List<ActividadLibreDetalle> libres = new ArrayList<>();
    private final List<SesionFuerzaDetalle> fuerzas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        props = new RetoPointsProperties();
        service = new RetoAdminActivityService(
                null, null, null, null, null, null, null, null, props);
    }

    @Test
    @DisplayName("El historial del día combinado suma 41, igual que el scoring")
    void historialDelEjemploCombinado() {
        fuerza(ANA, 0, 50, true, 10L);
        libre(ANA, 0, FreeActivityType.CAMINATA, 30, 20L);

        var historial = service.construirHistorial(
                ANA, "Ana", INICIO, INICIO.plusDays(6), INICIO,
                libres, fuerzas, List.of(), List.of(), Map.of());

        assertEquals(41, historial.getTotalPoints(), 0.5);
        var hoy = historial.getDays().stream().filter(d -> "Hoy".equals(d.getLabel())).findFirst().orElseThrow();
        double actividades = hoy.getEntries().stream()
                .filter(e -> "ACTIVITY".equals(e.getKind()))
                .mapToDouble(e -> e.getPoints())
                .sum();
        double constancia = hoy.getEntries().stream()
                .filter(e -> "CONSTANCIA".equals(e.getKind()))
                .mapToDouble(e -> e.getPoints())
                .sum();
        assertEquals(31, actividades, 0.5);
        assertEquals(10, constancia, 0.5);
    }

    @Test
    @DisplayName("La cuarta actividad del día aparece en el historial sin puntos")
    void cuartaActividadNoPuntua() {
        for (int i = 0; i < 4; i++) {
            libre(ANA, 0, FreeActivityType.CAMINATA, 30, 100L + i);
        }

        var historial = service.construirHistorial(
                ANA, "Ana", INICIO, INICIO.plusDays(6), INICIO,
                libres, fuerzas, List.of(), List.of(), Map.of());

        assertEquals(10 + 9 * 3, historial.getTotalPoints(), 0.5);
        long sinPuntos = historial.getDays().get(0).getEntries().stream()
                .filter(e -> "ACTIVITY".equals(e.getKind()) && e.getPoints() == 0)
                .count();
        assertEquals(1, sinPuntos);
    }

    @Test
    @DisplayName("Duplicar minutos en la segunda semana suma el bono de progreso")
    void historialConProgresoSemanal() {
        libre(ANA, 0, FreeActivityType.CAMINATA, 30, 1L);
        libre(ANA, 7, FreeActivityType.CAMINATA, 60, 2L);

        var historial = service.construirHistorial(
                ANA, "Ana", INICIO, INICIO.plusDays(13), INICIO.plusDays(13),
                libres, fuerzas, List.of(), List.of(), Map.of());

        assertEquals(19 + 28 + 15, historial.getTotalPoints(), 0.5);
        assertTrue(historial.getDays().stream().anyMatch(d -> d.getLabel().contains("progreso")));
    }

    @Test
    @DisplayName("Sin foto y duplicadas cercanas entran a la cola de revisión")
    void colaDetectaEvidenciaYDuplicado() {
        LocalDateTime t = INICIO.atTime(10, 0);
        libres.add(new ActividadLibreDetalle(
                1L, ANA, t, t.plusMinutes(30), FreeActivityType.CAMINATA, null, 1800, null, null));
        libres.add(new ActividadLibreDetalle(
                2L, ANA, t.plusMinutes(5), t.plusMinutes(35), FreeActivityType.CAMINATA, null, 1800, null, "https://foto"));

        var cola = service.construirCola(libres, fuerzas, Map.of(), Map.of(ANA, "Ana"));

        assertTrue(cola.stream().anyMatch(i -> i.getSuggestedFlags().contains(RetoActivityReviewFlag.MISSING_EVIDENCE)));
        assertTrue(cola.stream().anyMatch(i -> i.getSuggestedFlags().contains(RetoActivityReviewFlag.DUPLICATE)));
        assertTrue(cola.stream().anyMatch(i -> i.getSuggestedFlags().contains(RetoActivityReviewFlag.OUTSIDE_RULES)));
        assertTrue(cola.stream().allMatch(i -> i.getSource() == RetoActivitySource.FREE_ACTIVITY));
    }

    private void libre(Long userId, int diaOffset, FreeActivityType tipo, int minutos, long id) {
        LocalDateTime completed = INICIO.plusDays(diaOffset).atTime(10, 0);
        libres.add(new ActividadLibreDetalle(
                id, userId, completed.minusMinutes(minutos), completed, tipo, null,
                minutos * 60, null, "https://foto.test/evidencia.jpg"));
    }

    private void fuerza(Long userId, int diaOffset, int minutos, boolean completada, long id) {
        LocalDateTime inicio = INICIO.plusDays(diaOffset).atTime(8, 0);
        fuerzas.add(new SesionFuerzaDetalle(
                id, userId, inicio, inicio.plusMinutes(minutos),
                completada ? 100.0 : 60.0, 3));
    }
}
