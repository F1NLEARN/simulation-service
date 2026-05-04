package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AiAnalysisDetailResponse(
        UUID aiAnalysisId,
        UUID accountId,
        UUID targetUserId,
        String targetUserName,
        UUID seasonId,
        int seasonNumber,
        BigDecimal riskScore,
        BigDecimal portfolioConcentrationScore,
        String recommendedLearningTopic,
        String aiFeedbackMessage,
        LocalDateTime analysisPeriodStartAt,
        LocalDateTime analysisPeriodEndAt,
        LocalDateTime analyzedAt,
        AnalysisStatus analysisStatus
) {
    public static AiAnalysisDetailResponse from(AiAnalysis aiAnalysis) {
        return new AiAnalysisDetailResponse(
                aiAnalysis.getAiAnalysisId(),
                aiAnalysis.getAccountId(),
                aiAnalysis.getTargetUserId(),
                aiAnalysis.getTargetUserName(),
                aiAnalysis.getSeasonId(),
                aiAnalysis.getSeasonNumber(),
                aiAnalysis.getRiskScore().getValue(),
                aiAnalysis.getPortfolioConcentrationScore().getValue(),
                aiAnalysis.getRecommendedLearningTopic(),
                aiAnalysis.getAiFeedbackMessage(),
                aiAnalysis.getAnalysisPeriodStartAt(),
                aiAnalysis.getAnalysisPeriodEndAt(),
                aiAnalysis.getAnalyzedAt(),
                aiAnalysis.getAnalysisStatus()
        );
    }
}