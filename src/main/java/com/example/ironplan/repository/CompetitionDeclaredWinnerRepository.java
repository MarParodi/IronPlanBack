package com.example.ironplan.repository;

import com.example.ironplan.model.CompetitionDeclaredWinner;
import com.example.ironplan.model.ParticipanteCategoria;
import com.example.ironplan.model.PodiumScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionDeclaredWinnerRepository extends JpaRepository<CompetitionDeclaredWinner, Long> {

    List<CompetitionDeclaredWinner> findByCompetitionIdOrderByScopeAscLevelCategoryAsc(Long competitionId);

    Optional<CompetitionDeclaredWinner> findByCompetitionIdAndScopeAndLevelCategory(
            Long competitionId, PodiumScope scope, ParticipanteCategoria levelCategory);

    Optional<CompetitionDeclaredWinner> findByCompetitionIdAndScopeAndLevelCategoryIsNull(
            Long competitionId, PodiumScope scope);
}
