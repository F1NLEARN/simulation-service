package com.finlearn.simulationservice.domain.analysis.repository;

import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    List<AiAnalysis> findAllByAccountId(UUID accountId);

    List<AiAnalysis> findAllByAccountIdAndSeasonId(UUID accountId, UUID seasonId);
}