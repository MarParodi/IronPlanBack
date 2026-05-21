package com.example.ironplan.rest.dto;

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
        private int memberCount;
        private int activeCompetitionsCount;
        private HierarchyPath hierarchyPath;
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
}
