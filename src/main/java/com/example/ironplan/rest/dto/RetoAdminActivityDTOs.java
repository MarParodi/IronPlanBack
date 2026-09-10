package com.example.ironplan.rest.dto;

import com.example.ironplan.model.RetoActivityReviewFlag;
import com.example.ironplan.model.RetoActivityReviewStatus;
import com.example.ironplan.model.RetoActivitySource;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RetoAdminActivityDTOs {

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PointHistory {
        private Long userId;
        private String fullName;
        private double totalPoints;
        private List<DayGroup> days;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DayGroup {
        private LocalDate date;
        private String label;
        @Builder.Default
        private List<HistoryEntry> entries = new ArrayList<>();
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class HistoryEntry {
        private String kind;
        private RetoActivitySource source;
        private Long sourceId;
        private String activityType;
        private String activityLabel;
        private Integer durationMinutes;
        private LocalDateTime occurredAt;
        private double points;
        private String ruleApplied;
        private String dataSource;
        private String validationStatus;
        private String evidenceUrl;
        @Builder.Default
        private List<RetoActivityReviewFlag> adminFlags = new ArrayList<>();
        private String adminNote;
        private RetoActivityReviewStatus reviewStatus;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ActivityReviewQueue {
        @Builder.Default
        private List<ActivityReviewItem> items = new ArrayList<>();
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ActivityReviewItem {
        private Long userId;
        private String fullName;
        private RetoActivitySource source;
        private Long sourceId;
        private String activityType;
        private String activityLabel;
        private Integer durationMinutes;
        private LocalDateTime occurredAt;
        private String evidenceUrl;
        @Builder.Default
        private List<RetoActivityReviewFlag> suggestedFlags = new ArrayList<>();
        @Builder.Default
        private List<RetoActivityReviewFlag> adminFlags = new ArrayList<>();
        private String adminNote;
        private RetoActivityReviewStatus reviewStatus;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class UpsertReviewRequest {
        @NotNull
        private RetoActivitySource source;
        @NotNull
        private Long sourceId;
        private List<RetoActivityReviewFlag> flags = new ArrayList<>();
        private String note;
        private RetoActivityReviewStatus status;
    }
}
