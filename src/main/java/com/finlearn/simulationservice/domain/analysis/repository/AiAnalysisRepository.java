package com.finlearn.simulationservice.domain.analysis.repository;

import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiAnalysisRepository {

    AiAnalysis save(AiAnalysis aiAnalysis);

    Optional<AiAnalysis> findById(UUID aiAnalysisId);

    List<AiAnalysis> findAllByAccountId(UUID accountId);

    List<AiAnalysis> findAllByAccountIdOrderByAnalyzedAtDesc(UUID accountId);

    Optional<AiAnalysis> findTopByAccountIdOrderByAnalyzedAtDesc(UUID accountId);

    Optional<AiAnalysis> findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(UUID accountId, AnalysisStatus analysisStatus);

    List<AiAnalysis> findAllByAccountIdAndSeasonId(UUID accountId, UUID seasonId);
}