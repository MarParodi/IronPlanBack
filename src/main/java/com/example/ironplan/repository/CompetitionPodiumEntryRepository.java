package com.example.ironplan.repository;

import com.example.ironplan.model.CompetitionPodiumEntry;
import com.example.ironplan.model.ParticipanteCategoria;
import com.example.ironplan.model.PodiumScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompetitionPodiumEntryRepository extends JpaRepository<CompetitionPodiumEntry, Long> {

    List<CompetitionPodiumEntry> findByCompetitionIdOrderByScopeAscLevelCategoryAscRankPositionAsc(Long competitionId);

    List<CompetitionPodiumEntry> findByCompetitionIdAndScopeAndLevelCategoryOrderByRankPositionAsc(
            Long competitionId, PodiumScope scope, ParticipanteCategoria levelCategory);

    List<CompetitionPodiumEntry> findByCompetitionIdAndScopeOrderByRankPositionAsc(
            Long competitionId, PodiumScope scope);

    Optional<CompetitionPodiumEntry> findByCompetitionIdAndScopeAndLevelCategoryAndUserId(
            Long competitionId, PodiumScope scope, ParticipanteCategoria levelCategory, Long userId);

    Optional<CompetitionPodiumEntry> findByCompetitionIdAndScopeAndLevelCategoryIsNullAndUserId(
            Long competitionId, PodiumScope scope, Long userId);

    @Modifying
    @Query("DELETE FROM CompetitionPodiumEntry e WHERE e.competition.id = :competitionId")
    void deleteAllByCompetitionId(@Param("competitionId") Long competitionId);

    boolean existsByCompetitionId(Long competitionId);
}
