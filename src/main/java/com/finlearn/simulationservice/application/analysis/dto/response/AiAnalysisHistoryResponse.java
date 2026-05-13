package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AiAnalysisHistoryResponse(
        UUID aiAnalysisId,
        AnalysisType analysisType,
        AnalysisStatus analysisStatus,
        String summary,
        BigDecimal riskScore,
        BigDecimal portfolioConcentrationScore,
        String recommendedLearningTopic,
        LocalDateTime analyzedAt,
        String failureReason
) {
    public static AiAnalysisHistoryResponse from(AiAnalysis aiAnalysis) {
        return new AiAnalysisHistoryResponse(
                aiAnalysis.getAiAnalysisId(),
                aiAnalysis.getAnalysisType(),
                aiAnalysis.getAnalysisStatus(),
                aiAnalysis.getSummary(),
                aiAnalysis.getRiskScore().getValue(),
                aiAnalysis.getPortfolioConcentrationScore().getValue(),
                aiAnalysis.getRecommendedLearningTopic(),
                aiAnalysis.getAnalyzedAt(),
                aiAnalysis.getFailureReason()
        );
    }
}
