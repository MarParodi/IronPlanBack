package com.example.ironplan.rest.dto;
 
import com.example.ironplan.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;
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

        /** GROUP = grupo vs grupo; ORGANIZATION_MEMBERS = ranking individual org-wide */
        private ParticipantMode participantMode;
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
        @JsonProperty("isMemberCompetition")
        private boolean           isMemberCompetition; // true si scopeLevel = GRUPO o participantMode = ORGANIZATION_MEMBERS
        private ParticipantMode   participantMode;
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
        private Level level;
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
        private Long    participantGroupId;

        // Competencia individual
        private Integer memberRank;
        private Double  individualScore;
 
        @JsonProperty("isMemberCompetition")
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

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class WinnerInfo {
        private Long   id;
        private String name;
        private Double score;
        private boolean tie;
        private String type; // GROUP | MEMBER
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class RetoSummary {
        private Long              id;
        private String            name;
        private CompetitionType   competitionType;
        private ScopeLevel        scopeLevel;
        private MetricType        metricType;
        private LocalDate         startDate;
        private LocalDate         endDate;
        private CompetitionStatus status;
        private int               participantCount;
        @JsonProperty("isMemberCompetition")
        private boolean           isMemberCompetition;
        private WinnerInfo        leader;
        private String            metricLabel;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class CompetitionDetailView {
        private Response competition;
        private List<LeaderboardEntry> groupLeaderboard;
        private List<MemberLeaderboardEntry> memberLeaderboard;
        private List<InternalRankingEntry> internalRanking;
        private WinnerInfo winner;
        private MyScore myScore;
        private String metricLabel;
        private LocalDateTime lastCalculatedAt;
        private PodiumsResponse podiums;
        private List<DeclaredWinnerDto> declaredWinners;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PodiumEntryDto {
        private int rank;
        private Long userId;
        private String fullName;
        private String username;
        private String profilePictureUrl;
        private String levelCategory;
        private Double compositeScore;
        private Double consistencyRaw;
        private Double oneRmProgressRaw;
        private Double volumeRaw;
        private Double consistencyNorm;
        private Double oneRmNorm;
        private Double volumeNorm;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PodiumsResponse {
        private LocalDateTime generated;
        private List<PodiumEntryDto> generalTop3;
        private java.util.Map<String, List<PodiumEntryDto>> byLevel;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DeclaredWinnerDto {
        private String scope;
        private String levelCategory;
        private String levelLabel;
        private Long userId;
        private String fullName;
        private String username;
        private String profilePictureUrl;
        private LocalDateTime declaredAt;
    }

    @Getter @Setter
    public static class DeclareWinnerRequest {
        @NotNull
        private PodiumScope scope;
        private ParticipanteCategoria levelCategory;
        @NotNull
        private Long userId;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AdminRetoDashboard {
        private Response competition;
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private int weekIndex;
        private Double gapFirstSecond;
        private AdminRetoKpis kpis;
        private List<AdminRetoTeam> teams;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AdminRetoKpis {
        private int activeThisWeek;
        private int totalActivities;
        private double pointsToday;
        private double pointsThisWeek;
        private double avgPointsPerMember;
        private double contributionPercent;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AdminRetoTeam {
        private Long groupId;
        private String groupName;
        private int rank;
        private double score;
        private double gapFromLeader;
        private double fuerzaPoints;
        private double librePoints;
        private double teamBonusPoints;
        private int rosterSize;
        private int activeThisWeek;
        private double participationPercent;
        private double contributionPercent;
        private List<AdminRetoMember> members;
        private int totalActivities;
        private double pointsToday;
        private double pointsThisWeek;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AdminRetoMember {
        private Long userId;
        private String fullName;
        private double points;
        private double fuerza;
        private double libre;
        private int activeDays;
        private LocalDateTime lastActivityAt;
    }
}