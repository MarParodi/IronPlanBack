package com.example.ironplan.repository;
 
import com.example.ironplan.model.Competition;
import com.example.ironplan.model.CompetitionStatus;
import com.example.ironplan.model.CompetitionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.time.LocalDate;
import java.util.List;
 
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
 
    List<Competition> findByStatus(CompetitionStatus status);
 
    List<Competition> findByCompetitionType(CompetitionType type);
 
    List<Competition> findByStatusAndCompetitionType(CompetitionStatus status, CompetitionType type);
 
    // Competencias activas que expiaron (para el scheduler)
    List<Competition> findByStatusAndEndDateBefore(CompetitionStatus status, LocalDate date);
 
    // Competencias activas donde participa un grupo específico
    @Query("""
        SELECT c FROM Competition c
        JOIN c.participants p
        WHERE c.status = 'ACTIVE'
        AND p.group.id = :groupId
    """)
    List<Competition> findActiveByParticipantGroup(@Param("groupId") Long groupId);
 
    // Competencias activas visibles para un grupo: grupal (como participante) o intra-grupo (scope en este nodo)
    @Query("""
        SELECT DISTINCT c FROM Competition c
        LEFT JOIN c.participants p
        WHERE c.status = 'ACTIVE'
        AND (
            p.group.id = :groupId
            OR p.group.id = (SELECT g.parent.id FROM OrganizationalGroup g WHERE g.id = :groupId)
            OR p.group.id = (SELECT g.parent.parent.id FROM OrganizationalGroup g WHERE g.id = :groupId)
            OR p.group.id = (SELECT g.parent.parent.parent.id FROM OrganizationalGroup g WHERE g.id = :groupId)
            OR (c.scopeLevel = com.example.ironplan.model.ScopeLevel.GRUPO AND c.scopeReference.id = :groupId)
        )
        """)
    List<Competition> findActiveCompetitionsForGroup(@Param("groupId") Long groupId);
    
    
    
    
    @Query("""
    	    SELECT DISTINCT c FROM Competition c
    	    JOIN c.participants p
    	    WHERE p.group.id IN :groupIds
    	    ORDER BY c.createdAt DESC
    	""")
    	List<Competition> findAllByParticipantGroupIds(@Param("groupIds") List<Long> groupIds);

    @Query("""
        SELECT DISTINCT c FROM Competition c
        WHERE c.status IN :statuses
        AND (
            c.scopeReference.id IN :groupIds
            OR EXISTS (
                SELECT 1 FROM CompetitionParticipant p
                WHERE p.competition = c AND p.group.id IN :groupIds
            )
            OR (
                c.scopeLevel = com.example.ironplan.model.ScopeLevel.GRUPO
                AND EXISTS (
                    SELECT 1 FROM CompetitionMemberParticipant mp
                    JOIN OrganizationalGroupMember m ON m.user = mp.user AND m.active = true
                    WHERE mp.competition = c AND m.group.id IN :groupIds
                )
            )
        )
        ORDER BY c.createdAt DESC
        """)
    List<Competition> findVisibleForOrganizationalScope(
        @Param("groupIds") List<Long> groupIds,
        @Param("statuses") List<CompetitionStatus> statuses
    );

    @Query("""
        SELECT c FROM Competition c
        JOIN CompetitionMemberParticipant mp ON mp.competition = c
        WHERE c.status = 'ACTIVE' AND mp.user.id = :userId
        """)
    List<Competition> findActiveMemberCompetitionsForUser(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT c FROM Competition c
        JOIN CompetitionMemberParticipant mp ON mp.competition = c
        WHERE mp.user.id = :userId
        ORDER BY c.createdAt DESC
        """)
    List<Competition> findAllMemberCompetitionsForUser(@Param("userId") Long userId);
}