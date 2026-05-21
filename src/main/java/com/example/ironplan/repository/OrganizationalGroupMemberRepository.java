package com.example.ironplan.repository;

import com.example.ironplan.model.GroupMembershipRole;
import com.example.ironplan.model.OrganizationalGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationalGroupMemberRepository extends JpaRepository<OrganizationalGroupMember, Long> {

    List<OrganizationalGroupMember> findByUserIdAndActiveTrue(Long userId);

    List<OrganizationalGroupMember> findByUserIdAndRoleAndActiveTrue(Long userId, GroupMembershipRole role);

    Optional<OrganizationalGroupMember> findByUserIdAndGroupIdAndActiveTrue(Long userId, Long groupId);

    Optional<OrganizationalGroupMember> findByUserIdAndGroupId(Long userId, Long groupId);

    boolean existsByUserIdAndGroupIdAndActiveTrue(Long userId, Long groupId);

    long countByGroupIdAndActiveTrueAndRole(Long groupId, GroupMembershipRole role);

    @Query("""
        SELECT DISTINCT m.user.id FROM OrganizationalGroupMember m
        WHERE m.active = true AND m.group.id IN :groupIds
        """)
    List<Long> findDistinctActiveUserIdsByGroupIds(@Param("groupIds") List<Long> groupIds);

    @Query("""
        SELECT COUNT(m) > 0 FROM OrganizationalGroupMember m
        WHERE m.user.id = :userId AND m.active = true
        AND m.group.id IN :groupIds
        """)
    boolean existsActiveMembershipInGroups(@Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);

    @Query("""
        SELECT m FROM OrganizationalGroupMember m
        JOIN FETCH m.user u
        JOIN FETCH m.group g
        LEFT JOIN FETCH g.parent
        WHERE m.group.id = :groupId AND m.active = true
        ORDER BY m.role ASC, m.joinedAt ASC
        """)
    List<OrganizationalGroupMember> findActiveByGroupIdWithGroup(@Param("groupId") Long groupId);

    long countByGroupIdAndActiveTrue(Long groupId);
}
