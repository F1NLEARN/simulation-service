package com.finlearn.simulationservice.domain.analysis.command;

import com.finlearn.simulationservice.domain.analysis.entity.AnalysisType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAiAnalysisCommand(
        UUID accountId,
        UUID targetUserId,
        String targetUserName,
        UUID seasonId,
        int seasonNumber,
        AnalysisType analysisType,
        BigDecimal riskScore,
        BigDecimal portfolioConcentrationScore,
        String recommendedLearningTopic,
        String summary,
        String aiFeedbackMessage,
        LocalDateTime analysisPeriodStartAt,
        LocalDateTime analysisPeriodEndAt,
        LocalDateTime analyzedAt,
        String prompt,         // nullable, 디버깅용
        String modelResponse   // nullable, 디버깅용
) {
}