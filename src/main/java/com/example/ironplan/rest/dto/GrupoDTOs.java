package com.example.ironplan.rest.dto;

import com.example.ironplan.model.CompetitionType;
import com.example.ironplan.model.MetricType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GrupoDTOs {

    @Getter @Setter @Builder
    public static class HierarchyPath {
        private List<Long> ancestorIds;
        private String rootName;
        private String middlePath;
        private String leafName;
        private String displayPath;
    }

    @Getter @Setter @Builder
    public static class MembershipSummary {
        private Long groupId;
        private String groupName;
        private String groupCode;
        private String groupType;
        private String role;
        private boolean canManage;
        private boolean canLeave;
        private int memberCount;
        private int activeCompetitionsCount;
        private HierarchyPath hierarchyPath;
    }

    @Getter @Setter @Builder
    public static class GroupDetail {
        private Long groupId;
        private String groupName;
        private String groupCode;
        private String groupType;
        private boolean active;
        private String role;
        private boolean canManage;
        private boolean canLeave;
        private int memberCount;
        private int activeCompetitionsCount;
        private HierarchyPath hierarchyPath;
        private String photoUrl;
    }

    @Getter @Setter @Builder
    public static class MemberItem {
        private Long userId;
        private String fullName;
        private String username;
        private String role;
        private LocalDateTime joinedAt;
        private String profilePictureUrl;
        private Long groupId;
        private String groupName;
        /** Nivel de entrenamiento del perfil: NOVATO, INTERMEDIO, AVANZADO */
        private String level;
    }

    @Getter @Setter
    public static class AddMemberRequest {
        /** Username o email del usuario */
        private String identifier;
        private String membershipRole;
    }

    @Getter @Setter
    public static class UpdateMemberRoleRequest {
        private String role;
    }

    @Getter @Setter @Builder
    public static class GroupMetrics {
        private int periodDays;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private int totalMembers;
        private int activeMembers;
        private double adherencePercent;
        private double participationPercent;
        private long totalWorkouts;
        private long totalSessions;
        private long totalActiveMinutes;
        private double avgWorkoutsPerActiveMember;
        private List<WeeklyMetricPoint> weeklyWorkouts;
        private List<ParticipantMetricRank> topParticipants;
        private List<ObjectiveDistributionPoint> objectiveDistribution;
    }

    @Getter @Setter @Builder
    public static class ObjectiveDistributionPoint {
        private String objective;
        private String label;
        private long count;
    }

    @Getter @Setter @Builder
    public static class WeeklyMetricPoint {
        private LocalDate weekStart;
        private String weekLabel;
        private long workouts;
        private int activeMembers;
    }

    @Getter @Setter @Builder
    public static class ParticipantMetricRank {
        private int rank;
        private Long userId;
        private String fullName;
        private String username;
        private String profilePictureUrl;
        private long workouts;
        private long activeMinutes;
        private double score;
    }

    /** Crear un reto entre miembros del mismo grupo (scopeLevel = GRUPO). */
    @Getter @Setter
    public static class CreateRetoRequest {
        @NotBlank
        private String name;

        @NotNull
        private CompetitionType competitionType;

        @NotNull
        private MetricType metricType;

        @NotNull
        private LocalDate startDate;

        /** Obligatorio para CHALLENGE; opcional para RANKING (permanente si null). */
        private LocalDate endDate;

        /** IDs de miembros del grupo. Si null o vacío, se inscriben todos los miembros activos. */
        private List<Long> participantUserIds;
    }
}
