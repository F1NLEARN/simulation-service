package com.finlearn.simulationservice.domain.analysis.vo;

public record PortfolioRecommendation(
        RecommendationType recommendationType,
        String targetCategory,
        String reason,
        String message
) {}
