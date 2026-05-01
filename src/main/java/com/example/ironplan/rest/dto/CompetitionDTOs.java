package com.example.ironplan.rest.dto;
 
import com.example.ironplan.model.*;
import jakarta.validation.constraints.*;
import lombok.*;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
 
public class CompetitionDTOs {
 
    // ─── Request: crear competencia ───────────────────────────────────────────
 
    @Getter @Setter
    public static class CreateRequest {
 
        @NotBlank
        private String name;
 
        @NotNull
        private CompetitionType competitionType;
 
        @NotNull
        private ScopeLevel scopeLevel;
 
        @NotNull
        private Long scopeReferenceId; // ID del nodo organizacional anfitrión
 
        @NotNull
        private MetricType metricType;
 
        @NotNull
        private LocalDate startDate;
 
        private LocalDate endDate; // null = permanente
 
        // Para RANKING/CHALLENGE grupal: IDs de grupos participantes (null = todos)
        private List<Long> participantGroupIds;
 
        // Para competencia INDIVIDUAL (scopeLevel = GRUPO):
        // IDs de usuarios participantes (null = todos los del grupo)
        private List<Long> participantUserIds;
    }
 
    // ─── Response: competencia ────────────────────────────────────────────────
 
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long              id;
        private String            name;
        private CompetitionType   competitionType;
        private ScopeLevel        scopeLevel;
        private Long              scopeReferenceId;
        private String            scopeReferenceName;
        private MetricType        metricType;
        private LocalDate         startDate;
        private LocalDate         endDate;
        private CompetitionStatus status;
        private LocalDateTime     createdAt;
        private int               participantCount;
        private boolean           isMemberCompetition; // true si scopeLevel = GRUPO
    }
 
    // ─── Response: leaderboard grupal ─────────────────────────────────────────
 
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LeaderboardEntry {
        private int    rank;
        private Long   groupId;
        private String groupName;
        private Double groupScore;
        private int    activeMembers;
    }
 
    // ─── Response: leaderboard individual ────────────────────────────────────
 
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MemberLeaderboardEntry {
        private int    rank;
        private Long   userId;
        private String fullName;
        private String username;
        private String profilePictureUrl;
        private Double score;
    }
 
    // ─── Response: ranking interno (miembros dentro de un grupo) ─────────────
 
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class InternalRankingEntry {
        private int    position;
        private Long   userId;
        private String fullName;
        private String username;
        private String profilePictureUrl;
        private Double score;
    }
 
    // ─── Response: mi score ───────────────────────────────────────────────────
 
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MyScore {
        // Competencia grupal
        private Integer groupRank;
        private Double  groupScore;
        private Integer internalRank;
        private String  groupName;
 
        // Competencia individual
        private Integer memberRank;
        private Double  individualScore;
 
        private boolean isMemberCompetition;
    }
 
    // ─── Response: detalle de nodo para navegación del selector ──────────────
 
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ScopeNodeDetail {
        private Long    id;
        private String  name;
        private String  groupType;
        private boolean isLeaf;      // true si es nivel 4 (GRUPO)
        private int     memberCount; // solo si isLeaf = true
        private List<ScopeNodeDetail> children;
    }
}