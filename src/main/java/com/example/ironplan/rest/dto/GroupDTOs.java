package com.example.ironplan.rest.dto;

import com.example.ironplan.model.GroupType;
import com.example.ironplan.model.OrganizationKind;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

public class GroupDTOs {

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank private String name;
        @NotNull private GroupType groupType;
        private Long parentId;
        private String code;
        private OrganizationKind organizationKind;
    }

    @Getter @Setter
    public static class UpdateRequest {
        private String name;
        private Long parentId;
        private Boolean active;
    }

    @Getter @Setter @Builder
    public static class Response {
        private Long id;
        private String name;
        private GroupType groupType;
        private Long parentId;
        private String parentName;
        private String code;
        private Boolean active;
        private LocalDateTime createdAt;
        private OrganizationKind organizationKind;
    }
}