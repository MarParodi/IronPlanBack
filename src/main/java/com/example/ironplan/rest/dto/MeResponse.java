package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Gender;
import com.example.ironplan.model.Goal;
import com.example.ironplan.model.Level;
import com.example.ironplan.model.PersonalObjective;
import com.example.ironplan.model.Role;
import com.example.ironplan.model.WeightUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MeResponse(
        Long id,
        String email,
        String username,
        Role role,
        LocalDate birthday,
        Integer xpPoints,
        Level level,
        Integer trainDays,
        Gender gender,
        LocalDateTime createdAt,
        String profilePictureUrl,
        Integer weight,
        Integer height,
        Goal goal,
        PersonalObjective personalObjective,
        String personalObjectiveOther,
        WeightUnit weightUnit,
        Long organizationalGroupId,
        String organizationalGroupName,
        List<Long> ancestorGroupIds,
        String organizationRootName,
        String organizationMiddlePath,
        boolean canManageOrganization,
        boolean hasOrganizationAccess
) {}
