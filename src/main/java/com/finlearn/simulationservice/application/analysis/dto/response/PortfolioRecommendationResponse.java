package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import com.finlearn.simulationservice.domain.analysis.vo.RecommendationType;

public record PortfolioRecommendationResponse(
        RecommendationType recommendationType,
        String targetCategory,
        String reason,
        String message
) {
    public static PortfolioRecommendationResponse from(PortfolioRecommendation recommendation) {
        return new PortfolioRecommendationResponse(
                recommendation.recommendationType(),
                recommendation.targetCategory(),
                recommendation.reason(),
                recommendation.message()
        );
    }
}
