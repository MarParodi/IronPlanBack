package com.example.ironplan.repository;

import com.example.ironplan.model.RetoActivityReview;
import com.example.ironplan.model.RetoActivitySource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetoActivityReviewRepository extends JpaRepository<RetoActivityReview, Long> {

    List<RetoActivityReview> findByCompetition_Id(Long competitionId);

    Optional<RetoActivityReview> findByCompetition_IdAndSourceAndSourceId(
            Long competitionId, RetoActivitySource source, Long sourceId);
}
