package com.example.ironplan.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class JoinOrganizationDTOs {

    @Getter @Setter
    public static class Request {
        @NotBlank private String code;
    }

    @Getter @Setter @Builder
    public static class CodePreviewResponse {
        private String code;
        private Long groupId;
        private String groupName;
        private String organizationRootName;
        private String membershipRole;
    }
}
