package com.finlearn.simulationservice.infrastructure.analysis.persistence;

import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiAnalysisJpaRepository extends JpaRepository<AiAnalysis, UUID>, AiAnalysisRepository {

    @Override
    List<AiAnalysis> findAllByAccountId(UUID accountId);

    @Override
    List<AiAnalysis> findAllByAccountIdOrderByAnalyzedAtDesc(UUID accountId);

    @Override
    Optional<AiAnalysis> findTopByAccountIdOrderByAnalyzedAtDesc(UUID accountId);

    @Override
    List<AiAnalysis> findAllByAccountIdAndSeasonId(UUID accountId, UUID seasonId);
}