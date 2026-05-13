package com.finlearn.simulationservice.domain.analysis.vo;

import java.util.List;

public record PortfolioDiagnosis(
        ConcentrationLevel concentrationLevel,
        RiskLevel riskLevel,
        String analysisSummary,
        List<String> warnings,
        List<PortfolioRecommendation> recommendations
) {}
