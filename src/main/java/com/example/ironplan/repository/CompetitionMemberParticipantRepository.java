package com.example.ironplan.repository;
 
import com.example.ironplan.model.CompetitionMemberParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.util.List;
import java.util.Optional;
 
public interface CompetitionMemberParticipantRepository
        extends JpaRepository<CompetitionMemberParticipant, Long> {
 
    List<CompetitionMemberParticipant> findByCompetitionIdOrderByRankAsc(Long competitionId);
 
    Optional<CompetitionMemberParticipant> findByCompetitionIdAndUserId(
            Long competitionId, Long userId);
 
    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);
 
    // Leaderboard individual ordenado por score
    @Query("""
        SELECT p FROM CompetitionMemberParticipant p
        JOIN FETCH p.user u
        WHERE p.competition.id = :competitionId
        ORDER BY p.score DESC
    """)
    List<CompetitionMemberParticipant> findLeaderboard(@Param("competitionId") Long competitionId);
 
    // Todos los participantes de una competencia individual
    // activos en ese grupo (para inscripción automática)
    @Query("""
        SELECT u FROM User u
        WHERE u.primaryOrganizationalGroup.id = :groupId
    """)
    List<CompetitionMemberParticipant> findMembersByGroup(@Param("groupId") Long groupId);
}