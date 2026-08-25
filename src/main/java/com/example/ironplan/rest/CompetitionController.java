package com.example.ironplan.rest;
 
import com.example.ironplan.model.User;
import com.example.ironplan.model.CompetitionStatus;
import com.example.ironplan.model.CompetitionType;
import com.example.ironplan.model.Level;
import com.example.ironplan.rest.dto.CompetitionDTOs;
import com.example.ironplan.service.CompetitionService;
import com.example.ironplan.service.CompetitionPodiumService;
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
    private final CompetitionPodiumService podiumService;
 
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
    
    @GetMapping("/{id}/participants/{groupId}/members")
    public ResponseEntity<List<CompetitionDTOs.ScopeNodeDetail>> getParticipantMembers(
        @PathVariable Long id,
        @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(competitionService.getMembersUnderGroup(groupId));
    }

    @PostMapping("/{id}/generate-podiums")
    public ResponseEntity<CompetitionDTOs.PodiumsResponse> generatePodiums(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(podiumService.generatePodiums(id, user));
    }

    @GetMapping("/{id}/podiums")
    public ResponseEntity<CompetitionDTOs.PodiumsResponse> getPodiums(@PathVariable Long id) {
        return ResponseEntity.ok(podiumService.getPodiums(id));
    }

    @PostMapping("/{id}/declare-winner")
    public ResponseEntity<CompetitionDTOs.DeclaredWinnerDto> declareWinner(
        @PathVariable Long id,
        @Valid @RequestBody CompetitionDTOs.DeclareWinnerRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(podiumService.declareWinner(id, request, user));
    }

    @GetMapping("/{id}/reto-dashboard")
    public ResponseEntity<CompetitionDTOs.AdminRetoDashboard> getRetoDashboard(
        @PathVariable Long id,
        @RequestParam(required = false) Long groupId
    ) {
        competitionService.ensureScoresFresh(id);
        return ResponseEntity.ok(competitionService.getRetoDashboard(id, groupId));
    }
}
 
// ─── Público (usuario autenticado) ────────────────────────────────────────────
 
@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
class PublicCompetitionController {
 
    private final CompetitionService competitionService;
    private final CompetitionPodiumService podiumService;
 
    @GetMapping("/active")
    public ResponseEntity<List<CompetitionDTOs.Response>> getActive(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(competitionService.getActiveForUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDTOs.CompetitionDetailView> getById(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        competitionService.ensureScoresFresh(id);
        return ResponseEntity.ok(competitionService.getDetailForUser(id, user));
    }

    // Leaderboard grupal
    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<CompetitionDTOs.LeaderboardEntry>> getLeaderboard(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        competitionService.ensureScoresFresh(id);
        return ResponseEntity.ok(competitionService.getLeaderboard(id, user));
    }

    // Leaderboard individual (scopeLevel = GRUPO)
    @GetMapping("/{id}/leaderboard/members")
    public ResponseEntity<List<CompetitionDTOs.MemberLeaderboardEntry>> getMemberLeaderboard(
        @PathVariable Long id,
        @RequestParam(required = false) Level level,
        @AuthenticationPrincipal User user
    ) {
        competitionService.ensureScoresFresh(id);
        return ResponseEntity.ok(competitionService.getMemberLeaderboard(id, user, level));
    }
 
    // Ranking interno del grupo del usuario
    @GetMapping("/{id}/leaderboard/internal")
    public ResponseEntity<List<CompetitionDTOs.InternalRankingEntry>> getInternalRanking(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        competitionService.ensureScoresFresh(id);
        return ResponseEntity.ok(competitionService.getInternalRanking(id, user));
    }
 
    // Mi score y posición
    @GetMapping("/{id}/my-score")
    public ResponseEntity<CompetitionDTOs.MyScore> getMyScore(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        competitionService.ensureScoresFresh(id);
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
    
    
    @GetMapping("/my-competitions")
    public ResponseEntity<List<CompetitionDTOs.Response>> getMyCompetitions(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(competitionService.getAllForUser(user));
    }

    @GetMapping("/{id}/podiums")
    public ResponseEntity<CompetitionDTOs.PodiumsResponse> getPodiums(@PathVariable Long id) {
        return ResponseEntity.ok(podiumService.getPodiums(id));
    }

    @GetMapping("/{id}/winners")
    public ResponseEntity<List<CompetitionDTOs.DeclaredWinnerDto>> getWinners(@PathVariable Long id) {
        return ResponseEntity.ok(podiumService.getDeclaredWinners(id));
    }
}
 