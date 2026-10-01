package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExperimentExportServiceTest {

    @Test
    @DisplayName("El CSV semanal incluye participantes sin post-test, con una fila por semana transcurrida")
    void csvSemanalSinFiltroPrePost() {
        ExperimentoRetoRepository retoRepo = mock(ExperimentoRetoRepository.class);
        ParticipanteRetoRepository participanteRepo = mock(ParticipanteRetoRepository.class);
        SnapshotSemanalUsuarioRepository snapshotRepo = mock(SnapshotSemanalUsuarioRepository.class);
        SnapshotService snapshotService = mock(SnapshotService.class);
        ExperimentExportService service = new ExperimentExportService(
                retoRepo, participanteRepo, mock(IpaqRespuestaRepository.class), mock(SusRespuestaRepository.class),
                snapshotRepo, mock(OrganizationalAccessService.class), snapshotService,
                mock(CompetitionParticipantRepository.class), mock(CompetitionMemberParticipantRepository.class));

        LocalDate inicio = LocalDate.of(2026, 3, 2);
        ExperimentoReto reto = ExperimentoReto.builder().id(1L).fechaInicio(inicio).fechaFin(inicio.plusWeeks(8)).build();
        when(retoRepo.findById(1L)).thenReturn(Optional.of(reto));
        when(snapshotService.semanasTranscurridas(any())).thenReturn(2);

        ParticipanteReto completo = participante(11L, 101L, true);
        ParticipanteReto sinPost = participante(12L, 102L, false);
        when(participanteRepo.findByRetoIdWithUsuario(1L)).thenReturn(List.of(completo, sinPost));
        when(snapshotRepo.findByRetoIdAndUsuarioIdOrderByNumeroSemanaAsc(1L, 101L))
                .thenReturn(List.of(snap(1, inicio), snap(2, inicio.plusWeeks(1)), snap(3, inicio.plusWeeks(2))));
        when(snapshotRepo.findByRetoIdAndUsuarioIdOrderByNumeroSemanaAsc(1L, 102L))
                .thenReturn(List.of(snap(1, inicio), snap(2, inicio.plusWeeks(1))));

        String csv = service.exportarCsvSemanal(1L, null);
        String[] filas = csv.split("\n");

        verify(snapshotService).actualizarSnapshotsPendientes(1L);
        assertEquals(5, filas.length);
        assertTrue(filas[0].startsWith("participante_id,grupo"));
        assertEquals(2, java.util.Arrays.stream(filas).filter(f -> f.startsWith("12,")).count());
        assertEquals(2, java.util.Arrays.stream(filas).filter(f -> f.startsWith("11,")).count());
    }

    private static ParticipanteReto participante(Long id, Long userId, boolean posttest) {
        User u = new User();
        u.setId(userId);
        u.setTrainDays(3);
        return ParticipanteReto.builder()
                .id(id).usuario(u).activo(true).completoPretest(true).completoPosttest(posttest).build();
    }

    private static SnapshotSemanalUsuario snap(int semana, LocalDate inicio) {
        return SnapshotSemanalUsuario.builder()
                .numeroSemana(semana).fechaInicioSemana(inicio).fechaFinSemana(inicio.plusDays(6))
                .sesionesCompletadas(1).sesionesTotales(2).diasActivos(2).semanaCompleta(true).activo(true)
                .build();
    }
}
