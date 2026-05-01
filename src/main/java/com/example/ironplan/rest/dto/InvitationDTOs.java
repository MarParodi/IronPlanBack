package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

public class InvitationDTOs {

    @Getter @Setter
    public static class CreateRequest {
        @NotNull private Long organizationalGroupId;
        private String code;
        private Integer maxUses;
        private LocalDate expiresAt;
    }

    @Getter @Setter @Builder
    public static class Response {
        private Long id;
        private String code;
        private Long groupId;
        private String groupName;
        private Integer maxUses;
        private Integer usesCount;
        private LocalDate expiresAt;
        private Boolean active;
    }
}