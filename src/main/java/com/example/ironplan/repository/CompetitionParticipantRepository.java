package com.example.ironplan.repository;
 
import com.example.ironplan.model.CompetitionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.util.List;
import java.util.Optional;
 
public interface CompetitionParticipantRepository extends JpaRepository<CompetitionParticipant, Long> {
 
    List<CompetitionParticipant> findByCompetitionIdOrderByRankAsc(Long competitionId);
 
    Optional<CompetitionParticipant> findByCompetitionIdAndGroupId(Long competitionId, Long groupId);
 
    boolean existsByCompetitionIdAndGroupId(Long competitionId, Long groupId);
 
    // Leaderboard grupal ordenado por score
    @Query("""
        SELECT p FROM CompetitionParticipant p
        WHERE p.competition.id = :competitionId
        ORDER BY p.groupScore DESC
    """)
    List<CompetitionParticipant> findLeaderboard(@Param("competitionId") Long competitionId);
}
 