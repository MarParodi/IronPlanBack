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
 
    // Competencias donde participa el usuario (vía su grupo)
    @Query("""
    	    SELECT c FROM Competition c
    	    JOIN c.participants p
    	    WHERE c.status = 'ACTIVE'
    	    AND (
    	        p.group.id = :groupId
    	        OR p.group.id = (SELECT g.parent.id FROM OrganizationalGroup g WHERE g.id = :groupId)
    	        OR p.group.id = (SELECT g.parent.parent.id FROM OrganizationalGroup g WHERE g.id = :groupId)
    	        OR p.group.id = (SELECT g.parent.parent.parent.id FROM OrganizationalGroup g WHERE g.id = :groupId)
    	    )
    	""")
    	List<Competition> findActiveCompetitionsForGroup(@Param("groupId") Long groupId);
}