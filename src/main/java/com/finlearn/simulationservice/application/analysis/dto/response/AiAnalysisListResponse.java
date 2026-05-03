package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AiAnalysisListResponse(
        UUID aiAnalysisId,
        UUID seasonId,
        int seasonNumber,
        BigDecimal riskScore,
        BigDecimal portfolioConcentrationScore,
        String recommendedLearningTopic,
        LocalDateTime analysisPeriodStartAt,
        LocalDateTime analysisPeriodEndAt,
        LocalDateTime analyzedAt,
        AnalysisStatus analysisStatus
) {
    public static AiAnalysisListResponse from(AiAnalysis aiAnalysis) {
        return new AiAnalysisListResponse(
                aiAnalysis.getAiAnalysisId(),
                aiAnalysis.getSeasonId(),
                aiAnalysis.getSeasonNumber(),
                aiAnalysis.getRiskScore().getValue(),
                aiAnalysis.getPortfolioConcentrationScore().getValue(),
                aiAnalysis.getRecommendedLearningTopic(),
                aiAnalysis.getAnalysisPeriodStartAt(),
                aiAnalysis.getAnalysisPeriodEndAt(),
                aiAnalysis.getAnalyzedAt(),
                aiAnalysis.getAnalysisStatus()
        );
    }
}