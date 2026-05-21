package com.example.ironplan.rest.dto;

import com.example.ironplan.model.GroupType;
import com.example.ironplan.model.OrganizationKind;
import lombok.*;

import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class GroupTreeDTO {
	
	private Long   id;
    private String name;
    private String code;
    private GroupType        groupType;
    private OrganizationKind organizationKind;
    private Boolean active;
    private Long   parentId;
    private String parentName;
    private int    totalMembers;
    private int    totalChildren;
    private List<GroupTreeDTO> children;

}
