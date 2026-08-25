package com.example.ironplan.config;

import com.example.ironplan.model.FreeActivityType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parámetros de la métrica {@code TEAM_POINTS}. Los defaults reproducen el reto
 * "Crecemos Más Fuertes, Crecemos Juntos"; cualquier otro reto puede ajustarlos
 * desde {@code application.properties} bajo el prefijo {@code ironplan.reto-points}.
 */
@Component
@ConfigurationProperties(prefix = "ironplan.reto-points")
@Getter
@Setter
public class RetoPointsProperties {

    // ── Constancia y esfuerzo ────────────────────────────────────────────────

    /** Puntos por día con al menos una actividad válida (una sola vez por día). */
    private double constanciaPorDia = 10;

    /** Puntos otorgados por cada bloque de minutos puntuables. */
    private double puntosPorBloque = 3;

    /** Tamaño del bloque de minutos usado para el esfuerzo. */
    private int minutosPorBloque = 10;

    /** Duración mínima para que una actividad sea válida. */
    private int minutosMinimos = 10;

    /** Minutos máximos puntuables por actividad; el resto se registra pero no suma. */
    private int minutosMaxPuntuables = 60;

    /** Actividades puntuables por día; las demás quedan solo en estadísticas. */
    private int maxActividadesDia = 3;

    /** Tope de puntos competitivos por usuario y día (constancia + esfuerzo). */
    private double topeDiario = 50;

    /** Extra por completar la rutina de fuerza al 100%. */
    private double bonoRutinaCompleta = 7;

    /** Si la actividad libre necesita foto de evidencia para puntuar. */
    private boolean requiereFotoActividadLibre = true;

    /** Trabajo efectivo mínimo para que una sesión de fuerza puntúe. */
    private int minEjerciciosCompletados = 1;

    /** Multiplicador de intensidad por tipo de actividad libre. */
    private Map<FreeActivityType, Double> intensidad = intensidadesPorDefecto();

    /** Multiplicador aplicado a un tipo de actividad sin intensidad configurada. */
    private double intensidadPorDefecto = 1.0;

    // ── Progreso semanal ─────────────────────────────────────────────────────

    /** Tope del bono de progreso por usuario y semana, sumando todas las modalidades. */
    private double progresoMaxSemanal = 15;

    /** Máximo del componente de actividad libre (minutos activos ponderados). */
    private double progresoLibreMax = 15;

    /** Máximo del componente de mejora de 1RM estimado. */
    private double progreso1rmMax = 7;

    /** Máximo del componente de mejora de volumen. */
    private double progresoVolumenMax = 8;

    /**
     * Tramos de mejora respecto al baseline personal. {@code desde} es el porcentaje
     * mínimo de mejora y {@code valor} la fracción del máximo que se otorga.
     */
    private List<Tramo> tramosProgreso = tramosProgresoPorDefecto();

    // ── Bonos de equipo ──────────────────────────────────────────────────────

    /** Días con actividad válida en la semana para contar como integrante activo. */
    private int diasActivosParaBonoEquipo = 3;

    /**
     * Tramos del bono de participación. {@code desde} es el porcentaje de integrantes
     * activos y {@code valor} los puntos que recibe el equipo esa semana.
     */
    private List<Tramo> tramosParticipacion = tramosParticipacionPorDefecto();

    /** Bono si el 100% del equipo registra al menos una actividad válida en la semana. */
    private double bonoNadieSeQuedaAtras = 25;

    // ── Helpers ──────────────────────────────────────────────────────────────

    public double intensidadDe(FreeActivityType tipo) {
        if (tipo == null) return intensidadPorDefecto;
        return intensidad.getOrDefault(tipo, intensidadPorDefecto);
    }

    /** Fracción del máximo que corresponde a una mejora porcentual dada. */
    public double fraccionProgreso(double mejoraPct) {
        return resolverTramo(tramosProgreso, mejoraPct, 0.0);
    }

    /** Puntos de participación colectiva para un porcentaje de integrantes activos. */
    public double puntosParticipacion(double porcentajeActivos) {
        return resolverTramo(tramosParticipacion, porcentajeActivos, 0.0);
    }

    private static double resolverTramo(List<Tramo> tramos, double valor, double porDefecto) {
        if (tramos == null || tramos.isEmpty()) return porDefecto;
        return tramos.stream()
                .filter(t -> valor >= t.getDesde())
                .max(Comparator.comparingDouble(Tramo::getDesde))
                .map(Tramo::getValor)
                .orElse(porDefecto);
    }

    private static Map<FreeActivityType, Double> intensidadesPorDefecto() {
        Map<FreeActivityType, Double> map = new EnumMap<>(FreeActivityType.class);
        // Baja
        map.put(FreeActivityType.CAMINATA, 1.0);
        map.put(FreeActivityType.CAMINADORA, 1.0);
        map.put(FreeActivityType.YOGA, 1.0);
        map.put(FreeActivityType.OTRA, 1.0);
        // Moderada
        map.put(FreeActivityType.BICICLETA_ESTATICA, 1.2);
        map.put(FreeActivityType.ELIPTICA, 1.2);
        map.put(FreeActivityType.BAILE, 1.2);
        map.put(FreeActivityType.CLASE_GRUPAL, 1.2);
        // Alta
        map.put(FreeActivityType.RUNNING, 1.4);
        map.put(FreeActivityType.NATACION, 1.4);
        map.put(FreeActivityType.FUTBOL, 1.4);
        map.put(FreeActivityType.BOX, 1.4);
        return map;
    }

    private static List<Tramo> tramosProgresoPorDefecto() {
        List<Tramo> tramos = new ArrayList<>();
        tramos.add(new Tramo(5, 0.4));
        tramos.add(new Tramo(10, 0.7));
        tramos.add(new Tramo(20, 1.0));
        return tramos;
    }

    private static List<Tramo> tramosParticipacionPorDefecto() {
        List<Tramo> tramos = new ArrayList<>();
        tramos.add(new Tramo(50, 20));
        tramos.add(new Tramo(70, 35));
        tramos.add(new Tramo(80, 50));
        tramos.add(new Tramo(90, 70));
        return tramos;
    }

    @Getter
    @Setter
    public static class Tramo {
        private double desde;
        private double valor;

        public Tramo() {
        }

        public Tramo(double desde, double valor) {
            this.desde = desde;
            this.valor = valor;
        }
    }
}
