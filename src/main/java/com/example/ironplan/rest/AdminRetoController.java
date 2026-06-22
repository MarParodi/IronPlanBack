package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.experimento.ExperimentoDTOs;
import com.example.ironplan.service.ExperimentExportService;
import com.example.ironplan.service.ExperimentoRetoService;
import com.example.ironplan.service.SnapshotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/retos")
@RequiredArgsConstructor
public class AdminRetoController {

    private final ExperimentoRetoService experimentoService;
    private final SnapshotService snapshotService;
    private final ExperimentExportService exportService;

    @GetMapping
    public ResponseEntity<List<ExperimentoDTOs.RetoResumenResponse>> list(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.listRetosParaAdmin(user));
    }

    @PostMapping
    public ResponseEntity<ExperimentoDTOs.RetoResumenResponse> create(
            @Valid @RequestBody ExperimentoDTOs.CreateRetoRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.crearReto(user, req));
    }

    @PatchMapping("/{retoId}/competition")
    public ResponseEntity<ExperimentoDTOs.RetoResumenResponse> vincularCompetition(
            @PathVariable Long retoId,
            @RequestBody ExperimentoDTOs.VincularCompetitionRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.vincularCompetition(retoId, user, req));
    }

    @GetMapping("/competencias-candidatas")
    public ResponseEntity<List<com.example.ironplan.rest.dto.CompetitionDTOs.Response>> competenciasPorOrg(
            @RequestParam Long organizacionId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.listCompetenciasCandidatasPorOrg(organizacionId, user));
    }

    @GetMapping("/{retoId}/competencias-candidatas")
    public ResponseEntity<List<com.example.ironplan.rest.dto.CompetitionDTOs.Response>> competenciasCandidatas(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.listCompetenciasCandidatas(retoId, user));
    }

    @DeleteMapping("/{retoId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        experimentoService.eliminarReto(retoId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{retoId}/experimento")
    public ResponseEntity<ExperimentoDTOs.ExperimentoEstadoResponse> estado(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.getEstadoAdmin(retoId, user));
    }

    @GetMapping("/{retoId}/sus/resumen")
    public ResponseEntity<ExperimentoDTOs.SusResumenResponse> susResumen(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.getSusResumenAdmin(retoId, user));
    }

    @PostMapping("/{retoId}/activar")
    public ResponseEntity<Void> activar(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        experimentoService.activarReto(retoId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{retoId}/activar-posttest")
    public ResponseEntity<Void> activarPosttest(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        experimentoService.activarPosttest(retoId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{retoId}/activar-sus")
    public ResponseEntity<Void> activarSus(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        experimentoService.activarSus(retoId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{retoId}/cerrar")
    public ResponseEntity<Void> cerrar(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        experimentoService.cerrarReto(retoId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{retoId}/snapshots/generar")
    public ResponseEntity<ExperimentoDTOs.SnapshotGenerarResponse> generarSnapshots(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(snapshotService.generarSnapshotSemanaActual(retoId, user));
    }

    @GetMapping("/{retoId}/exportar/csv")
    public ResponseEntity<String> exportarCsv(
            @PathVariable Long retoId,
            @RequestParam(defaultValue = "false") boolean incluirOutliers,
            @RequestParam(defaultValue = "true") boolean soloCompletos,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @AuthenticationPrincipal User user) {
        String csv = exportService.exportarCsvPrincipal(retoId, user, incluirOutliers, soloCompletos, incluirInactivos);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ironplan_datos_reto_" + retoId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
