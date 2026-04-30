package com.finlearn.simulationservice.domain.analysis.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioAnalysisCompletedEvent(
        UUID aiAnalysisId,
        UUID accountId,
        UUID seasonId,
        int seasonNumber,
        BigDecimal riskScore,
        BigDecimal portfolioConcentrationScore,
        LocalDateTime analyzedAt
) {
}