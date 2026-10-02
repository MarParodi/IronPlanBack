package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SnapshotServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 2);
    private static final LocalDate FIN = INICIO.plusDays(6);

    private ExperimentoRetoRepository retoRepo;
    private ParticipanteRetoRepository participanteRepo;
    private SnapshotSemanalUsuarioRepository snapshotRepo;
    private ProgressRepository progressRepo;
    private FreeActivitySessionRepository freeActivityRepo;
    private UserXpEventRepository xpEventRepo;
    private UserAchievementRepository achievementRepo;
    private RetoPointsScoringService scoringService;
    private SnapshotService service;

    @BeforeEach
    void setUp() {
        retoRepo = mock(ExperimentoRetoRepository.class);
        participanteRepo = mock(ParticipanteRetoRepository.class);
        snapshotRepo = mock(SnapshotSemanalUsuarioRepository.class);
        progressRepo = mock(ProgressRepository.class);
        freeActivityRepo = mock(FreeActivitySessionRepository.class);
        xpEventRepo = mock(UserXpEventRepository.class);
        achievementRepo = mock(UserAchievementRepository.class);
        scoringService = mock(RetoPointsScoringService.class);
        service = new SnapshotService(
                retoRepo, participanteRepo, snapshotRepo, progressRepo, freeActivityRepo,
                mock(CompetitionMemberParticipantRepository.class), xpEventRepo, achievementRepo,
                scoringService);
    }

    @Test
    @DisplayName("Los puntos del reto se calculan con las fechas del reto aunque la competencia tenga otras")
    void puntosConFechasDelReto() {
        ExperimentoReto reto = retoEnCurso();
        reto.setCompetition(Competition.builder()
                .id(4L).metricType(MetricType.TEAM_POINTS)
                .startDate(reto.getFechaInicio().plusMonths(3)).build());
        User user = usuario(3, 0);
        SnapshotSemanalUsuario previo = SnapshotSemanalUsuario.builder()
                .id(77L).numeroSemana(1).semanaCompleta(false).build();
        prepararMocks(reto, participante(user), previo);
        when(scoringService.scoreUsers(any(), eq(reto.getFechaInicio()), eq(reto.getFechaInicio().plusDays(6))))
                .thenReturn(java.util.Map.of(user.getId(), 41.0));

        service.actualizarSnapshotsPendientes(reto.getId());

        ArgumentCaptor<SnapshotSemanalUsuario> captor = ArgumentCaptor.forClass(SnapshotSemanalUsuario.class);
        verify(snapshotRepo).save(captor.capture());
        assertEquals(new BigDecimal("41.00"), captor.getValue().getPuntosRetoAcumulados());
        assertEquals(new BigDecimal("41.00"), captor.getValue().getPuntosRetoSemana());
    }

    @Test
    @DisplayName("Adherencia y días activos combinan fuerza y actividad libre del mismo día")
    void adherenciaYDiasActivos() {
        User user = usuario(3, 0);
        WorkoutSession ws = sesion(10L, INICIO.atTime(8, 0), INICIO.atTime(8, 50));
        var datos = datos(
                List.of(serie(ws, 50.0, 10), serie(ws, 70.0, 5)),
                List.of(libre(INICIO.atTime(18, 0), 1800), libre(INICIO.plusDays(2).atTime(18, 0), 1200)),
                List.of(), List.of(), List.of(), 0, 0);

        var s = service.construirSnapshot(reto(), participante(user), 1, INICIO, FIN, FIN.plusDays(1), datos);

        assertEquals(1, s.getSesionesCompletadas());
        assertEquals(2, s.getSesionesCardio());
        assertEquals(3, s.getSesionesTotales());
        assertEquals(new BigDecimal("3.00"), s.getSesionesPrevistas());
        assertEquals(new BigDecimal("100.00"), s.getAdherenciaPct());
        assertEquals(2, s.getDiasActivos());
        assertEquals(50, s.getMinutosFuerza());
        assertEquals(50, s.getMinutosCardio());
        assertEquals(new BigDecimal("60.00"), s.getCargaPromedio());
        assertTrue(s.getSemanaCompleta());
    }

    @Test
    @DisplayName("El rango y el XP se reconstruyen al cierre de la semana, no con el XP actual")
    void rangoHistorico() {
        User user = usuario(3, 1200);
        var datos = datos(List.of(), List.of(),
                List.of(xp(200), xp(-50)),
                List.of(xp(200), xp(-50), xp(100)),
                List.of(xp(300)),
                2, 5);

        var s = service.construirSnapshot(reto(), participante(user), 1, INICIO, FIN, FIN, datos);

        assertEquals(900, s.getXpAcumuladoAlFin());
        assertEquals(XpRank.NOVATO_I, s.getRangoFinSemana());
        assertEquals(200, s.getXpGanadoSemana());
        assertEquals(300, s.getXpAcumuladoPeriodo());
        assertEquals(2, s.getLogrosSemana());
        assertEquals(5, s.getLogrosAcumulados());
        assertFalse(s.getSemanaCompleta());
    }

    @Test
    @DisplayName("Las sesiones previstas se prorratean si la semana está recortada")
    void prorrateoSesionesPrevistas() {
        assertEquals(new BigDecimal("1.29"), SnapshotService.sesionesPrevistas(3, INICIO, INICIO.plusDays(2)));
        assertNull(SnapshotService.sesionesPrevistas(null, INICIO, FIN));
    }

    @Test
    @DisplayName("Recalcular una semana parcial actualiza el snapshot existente en vez de duplicarlo")
    void upsertSemanaParcial() {
        ExperimentoReto reto = retoEnCurso();
        User user = usuario(3, 0);
        ParticipanteReto p = participante(user);
        SnapshotSemanalUsuario previo = SnapshotSemanalUsuario.builder()
                .id(77L).numeroSemana(1).semanaCompleta(false).build();
        prepararMocks(reto, p, previo);

        service.actualizarSnapshotsPendientes(reto.getId());

        ArgumentCaptor<SnapshotSemanalUsuario> captor = ArgumentCaptor.forClass(SnapshotSemanalUsuario.class);
        verify(snapshotRepo).save(captor.capture());
        assertEquals(77L, captor.getValue().getId());
        assertEquals(1, captor.getValue().getNumeroSemana());
    }

    @Test
    @DisplayName("En modo pendientes no se recalculan las semanas ya completas")
    void pendientesOmiteSemanasCompletas() {
        ExperimentoReto reto = retoEnCurso();
        User user = usuario(3, 0);
        SnapshotSemanalUsuario previo = SnapshotSemanalUsuario.builder()
                .id(77L).numeroSemana(1).semanaCompleta(true).build();
        prepararMocks(reto, participante(user), previo);

        service.actualizarSnapshotsPendientes(reto.getId());

        verify(snapshotRepo, never()).save(any());
    }

    private void prepararMocks(ExperimentoReto reto, ParticipanteReto p, SnapshotSemanalUsuario previo) {
        when(retoRepo.findById(reto.getId())).thenReturn(Optional.of(reto));
        when(participanteRepo.findByRetoIdWithUsuario(reto.getId())).thenReturn(List.of(p));
        when(snapshotRepo.findByRetoIdAndUsuarioIdAndNumeroSemana(eq(reto.getId()), eq(p.getUsuario().getId()), eq(1)))
                .thenReturn(Optional.of(previo));
        when(progressRepo.findCompletedSetsInDateRange(anyLong(), any(), any())).thenReturn(List.of());
        when(freeActivityRepo.findByUser_IdAndCompletedAtBetweenOrderByCompletedAtDesc(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(xpEventRepo.findByUser_IdAndCreatedAtBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(xpEventRepo.findByUser_IdAndCreatedAtGreaterThanEqual(anyLong(), any())).thenReturn(List.of());
    }

    private static ExperimentoReto reto() {
        return ExperimentoReto.builder().id(1L).fechaInicio(INICIO).fechaFin(INICIO.plusWeeks(8)).semanasIntervencion(8).build();
    }

    private static ExperimentoReto retoEnCurso() {
        LocalDate inicio = LocalDate.now().minusDays(3);
        return ExperimentoReto.builder()
                .id(1L).fechaInicio(inicio).fechaFin(inicio.plusWeeks(8))
                .semanasIntervencion(8).estado(ExperimentoRetoEstado.ACTIVO).build();
    }

    private static User usuario(int trainDays, int lifetimeXp) {
        User u = new User();
        u.setId(5L);
        u.setTrainDays(trainDays);
        u.setLifetimeXp(lifetimeXp);
        return u;
    }

    private static ParticipanteReto participante(User user) {
        return ParticipanteReto.builder().id(9L).usuario(user).activo(true).build();
    }

    private static WorkoutSession sesion(Long id, LocalDateTime inicio, LocalDateTime fin) {
        WorkoutSession ws = new WorkoutSession();
        ws.setId(id);
        ws.setStartedAt(inicio);
        ws.setCompletedAt(fin);
        return ws;
    }

    private static WorkoutSet serie(WorkoutSession ws, double kg, int reps) {
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkoutSession(ws);
        WorkoutSet s = new WorkoutSet();
        s.setWorkoutExercise(we);
        s.setWeightKg(kg);
        s.setReps(reps);
        return s;
    }

    private static FreeActivitySession libre(LocalDateTime completedAt, int segundos) {
        return FreeActivitySession.builder().completedAt(completedAt).durationSeconds(segundos).build();
    }

    private static UserXpEvent xp(int delta) {
        UserXpEvent e = new UserXpEvent();
        e.setXpDelta(delta);
        return e;
    }

    private static SnapshotService.DatosSemana datos(
            List<WorkoutSet> sets, List<FreeActivitySession> libres,
            List<UserXpEvent> xpSemana, List<UserXpEvent> xpPeriodo, List<UserXpEvent> xpPosterior,
            long logrosSemana, long logrosAcumulados) {
        return new SnapshotService.DatosSemana(
                sets, libres, xpSemana, xpPeriodo, xpPosterior, logrosSemana, logrosAcumulados, null, null, null);
    }
}
