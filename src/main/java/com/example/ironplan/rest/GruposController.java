package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.CompetitionDTOs;
import com.example.ironplan.rest.dto.GrupoDTOs;
import com.example.ironplan.service.GruposService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
public class GruposController {

    private final GruposService gruposService;

    @GetMapping("/mis-grupos")
    public ResponseEntity<List<GrupoDTOs.MembershipSummary>> misGrupos(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gruposService.listMisGrupos(user));
    }

    @GetMapping("/administrar")
    public ResponseEntity<List<GrupoDTOs.MembershipSummary>> administrar(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gruposService.listAdministrar(user));
    }

    @GetMapping("/{groupId}/resumen")
    public ResponseEntity<GrupoDTOs.GroupDetail> resumen(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(gruposService.getDetail(groupId, user));
    }

    @GetMapping("/{groupId}/miembros")
    public ResponseEntity<List<GrupoDTOs.MemberItem>> miembros(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(gruposService.listMembers(groupId, user));
    }

    @PostMapping("/{groupId}/miembros")
    public ResponseEntity<GrupoDTOs.MemberItem> addMiembro(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @RequestBody GrupoDTOs.AddMemberRequest req
    ) {
        return ResponseEntity.ok(gruposService.addMember(groupId, user, req));
    }

    @PatchMapping("/{groupId}/miembros/{userId}")
    public ResponseEntity<GrupoDTOs.MemberItem> updateMiembroRol(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody GrupoDTOs.UpdateMemberRoleRequest req
    ) {
        return ResponseEntity.ok(gruposService.updateMemberRole(groupId, userId, user, req));
    }

    @DeleteMapping("/{groupId}/miembros/{userId}")
    public ResponseEntity<Void> removeMiembro(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @PathVariable Long userId
    ) {
        gruposService.removeMember(groupId, userId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/salir")
    public ResponseEntity<Void> salir(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId
    ) {
        gruposService.leaveGroup(groupId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/retos")
    public ResponseEntity<List<CompetitionDTOs.RetoSummary>> retos(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(gruposService.listRetos(groupId, user));
    }

    @GetMapping("/{groupId}/retos/{competitionId}")
    public ResponseEntity<CompetitionDTOs.CompetitionDetailView> retoDetalle(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @PathVariable Long competitionId
    ) {
        return ResponseEntity.ok(gruposService.getRetoDetail(groupId, competitionId, user));
    }

    @PostMapping("/{groupId}/retos")
    public ResponseEntity<CompetitionDTOs.Response> crearReto(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @Valid @RequestBody GrupoDTOs.CreateRetoRequest req
    ) {
        return ResponseEntity.ok(gruposService.createReto(groupId, user, req));
    }

    @PostMapping("/{groupId}/retos/{competitionId}/activate")
    public ResponseEntity<CompetitionDTOs.Response> activarReto(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @PathVariable Long competitionId
    ) {
        return ResponseEntity.ok(gruposService.activateReto(groupId, competitionId, user));
    }

    @GetMapping("/{groupId}/metricas")
    public ResponseEntity<GrupoDTOs.GroupMetrics> metricas(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(gruposService.getMetricas(groupId, user, days));
    }
}
