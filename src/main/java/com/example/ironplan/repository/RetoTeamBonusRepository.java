package com.example.ironplan.repository;

import com.example.ironplan.model.RetoTeamBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RetoTeamBonusRepository extends JpaRepository<RetoTeamBonus, Long> {

    List<RetoTeamBonus> findByCompetition_IdOrderByCreatedAtDesc(Long competitionId);

    @Query("""
            SELECT COALESCE(SUM(b.points), 0)
            FROM RetoTeamBonus b
            WHERE b.competition.id = :competitionId AND b.group.id = :groupId
            """)
    double sumPoints(@Param("competitionId") Long competitionId, @Param("groupId") Long groupId);
}
