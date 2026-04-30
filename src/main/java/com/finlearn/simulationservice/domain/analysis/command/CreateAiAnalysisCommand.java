package com.finlearn.simulationservice.domain.analysis.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAiAnalysisCommand(
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
        LocalDateTime analyzedAt
) {
}