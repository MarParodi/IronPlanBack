package com.example.ironplan.rest;
 
import com.example.ironplan.model.User;
import com.example.ironplan.model.CompetitionStatus;
import com.example.ironplan.model.CompetitionType;
import com.example.ironplan.rest.dto.CompetitionDTOs;
import com.example.ironplan.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
// ─── Admin ────────────────────────────────────────────────────────────────────
 
@RestController
@RequestMapping("/api/admin/competitions")
//@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminCompetitionController {
 
    private final CompetitionService competitionService;
 
    @GetMapping
    public ResponseEntity<List<CompetitionDTOs.Response>> getAll(
        @RequestParam(required = false) CompetitionStatus status,
        @RequestParam(required = false) CompetitionType type
    ) {
        return ResponseEntity.ok(competitionService.getAll(status, type));
    }
 
    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDTOs.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.getById(id));
    }
 
    @PostMapping
    public ResponseEntity<CompetitionDTOs.Response> create(
        @Valid @RequestBody CompetitionDTOs.CreateRequest req
    ) {
        return ResponseEntity.ok(competitionService.create(req));
    }
 
    @PostMapping("/{id}/activate")
    public ResponseEntity<CompetitionDTOs.Response> activate(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.activate(id));
    }
 
    @PostMapping("/{id}/finish")
    public ResponseEntity<CompetitionDTOs.Response> finish(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.finish(id));
    }
    
    @PostMapping("/{id}/recalculate")
    public ResponseEntity<Void> recalculate(@PathVariable Long id) {
        competitionService.recalculateScoresManual(id);
        return ResponseEntity.ok().build();
    }
}
 
// ─── Público (usuario autenticado) ────────────────────────────────────────────
 
@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
class PublicCompetitionController {
 
    private final CompetitionService competitionService;
 
    @GetMapping("/active")
    public ResponseEntity<List<CompetitionDTOs.Response>> getActive(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(competitionService.getActiveForUser(user));
    }
 
    // Leaderboard grupal
    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<CompetitionDTOs.LeaderboardEntry>> getLeaderboard(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(competitionService.getLeaderboard(id));
    }
 
    // Leaderboard individual (scopeLevel = GRUPO)
    @GetMapping("/{id}/leaderboard/members")
    public ResponseEntity<List<CompetitionDTOs.MemberLeaderboardEntry>> getMemberLeaderboard(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(competitionService.getMemberLeaderboard(id));
    }
 
    // Ranking interno del grupo del usuario
    @GetMapping("/{id}/leaderboard/internal")
    public ResponseEntity<List<CompetitionDTOs.InternalRankingEntry>> getInternalRanking(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(competitionService.getInternalRanking(id, user));
    }
 
    // Mi score y posición
    @GetMapping("/{id}/my-score")
    public ResponseEntity<CompetitionDTOs.MyScore> getMyScore(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(competitionService.getMyScore(id, user));
    }
 
    // Navegar jerarquía para el selector — hijos de un nodo
    @GetMapping("/scope/{groupId}/children")
    public ResponseEntity<List<CompetitionDTOs.ScopeNodeDetail>> getScopeChildren(
        @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(competitionService.getScopeChildren(groupId));
    }
 
    // Miembros de un grupo hoja (competencia individual)
    @GetMapping("/scope/{groupId}/members")
    public ResponseEntity<List<CompetitionDTOs.ScopeNodeDetail>> getGroupMembers(
        @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(competitionService.getGroupMembers(groupId));
    }
    
}
 