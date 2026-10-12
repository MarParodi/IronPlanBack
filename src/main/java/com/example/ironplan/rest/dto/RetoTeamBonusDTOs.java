package com.example.ironplan.rest.dto;

import lombok.*;

import java.time.LocalDateTime;

public class RetoTeamBonusDTOs {

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class BonusItem {
        private Long id;
        private Long groupId;
        private String groupName;
        private double points;
        private String title;
        private String evidenceUrl;
        private String createdByName;
        private LocalDateTime createdAt;
    }
}
