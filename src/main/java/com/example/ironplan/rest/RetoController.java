package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.experimento.ExperimentoDTOs;
import com.example.ironplan.service.ExperimentoRetoService;
import com.example.ironplan.service.SnapshotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retos")
@RequiredArgsConstructor
public class RetoController {

    private final ExperimentoRetoService experimentoService;
    private final SnapshotService snapshotService;

    @GetMapping("/activos")
    public ResponseEntity<List<ExperimentoDTOs.RetoResumenResponse>> listActivos(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.listRetosActivosParaUsuario(user));
    }

    @GetMapping("/{retoId}/mi-estado")
    public ResponseEntity<ExperimentoDTOs.ParticipanteStatusResponse> miEstado(
            @PathVariable Long retoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.getMiEstado(retoId, user));
    }

    @PostMapping("/{retoId}/inscripcion")
    public ResponseEntity<ExperimentoDTOs.InscripcionResponse> inscribir(
            @PathVariable Long retoId,
            @Valid @RequestBody ExperimentoDTOs.InscripcionRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.inscribir(retoId, user, req));
    }

    @PostMapping("/{retoId}/consentimiento")
    public ResponseEntity<Void> consentimiento(
            @PathVariable Long retoId,
            @Valid @RequestBody ExperimentoDTOs.ConsentimientoRequest req,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpReq) {
        if (req.ipDispositivo() == null || req.ipDispositivo().isBlank()) {
            req = new ExperimentoDTOs.ConsentimientoRequest(
                    req.participanteRetoId(), req.acepto(), httpReq.getRemoteAddr());
        }
        experimentoService.registrarConsentimiento(retoId, user, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{retoId}/ipaq")
    public ResponseEntity<ExperimentoDTOs.IpaqSubmitResponse> ipaq(
            @PathVariable Long retoId,
            @Valid @RequestBody ExperimentoDTOs.IpaqRequestDto req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.guardarIpaq(retoId, user, req));
    }

    @GetMapping("/{retoId}/ipaq/{participanteRetoId}")
    public ResponseEntity<ExperimentoDTOs.IpaqPairResponse> getIpaq(
            @PathVariable Long retoId,
            @PathVariable Long participanteRetoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(experimentoService.getIpaq(retoId, user, participanteRetoId));
    }

    @PostMapping("/{retoId}/sus")
    public ResponseEntity<ExperimentoDTOs.SusSubmitResponse> sus(
            @PathVariable Long retoId,
            @Valid @RequestBody ExperimentoDTOs.SusRequestDto req,
            @AuthenticationPrincipal User user) {
        experimentoService.requirePretestForUser(retoId, user);
        return ResponseEntity.ok(experimentoService.guardarSus(retoId, user, req));
    }

    @GetMapping("/{retoId}/snapshots/{usuarioId}")
    public ResponseEntity<List<com.example.ironplan.model.SnapshotSemanalUsuario>> snapshots(
            @PathVariable Long retoId,
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal User user) {
        if (!user.getId().equals(usuarioId)) {
            experimentoService.requirePretestForUser(retoId, user);
        }
        return ResponseEntity.ok(snapshotService.listSnapshots(retoId, usuarioId));
    }
}
