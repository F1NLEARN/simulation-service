package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public record PortfolioAnalysisResponse(
        UUID accountId,
        PortfolioSummaryResponse portfolioSummary,
        PortfolioAllocationResponse allocation,
        PortfolioDiagnosisResponse diagnosis,
        List<PortfolioRecommendationResponse> recommendations,
        List<PortfolioHoldingResponse> holdings
) {
    public static PortfolioAnalysisResponse of(InvestmentAccount account, List<Holding> holdings,
                                               PortfolioAllocationResponse allocation,
                                               PortfolioDiagnosis diagnosis,
                                               List<PortfolioRecommendation> recommendations,
                                               BigDecimal stockReturnRate,
                                               BigDecimal etfReturnRate) {
        long totalBuyAmount = holdings.stream()
                .mapToLong(Holding::getTotalBuyAmount)
                .sum();
        long totalValuationAmount = holdings.stream()
                .mapToLong(Holding::getValuationAmount)
                .sum();
        long totalProfitLoss = totalValuationAmount - totalBuyAmount;

        BigDecimal totalReturnRate = totalBuyAmount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(totalProfitLoss)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalBuyAmount), 2, RoundingMode.HALF_UP);

        List<PortfolioHoldingResponse> holdingResponses = holdings.stream()
                .map(h -> {
                    BigDecimal weight = totalValuationAmount == 0
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(h.getValuationAmount())
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(totalValuationAmount), 2, RoundingMode.HALF_UP);
                    return PortfolioHoldingResponse.from(h, weight);
                })
                .toList();

        PortfolioSummaryResponse summaryResponse = new PortfolioSummaryResponse(
                totalBuyAmount, totalValuationAmount, account.getCurrentCashBalance(),
                account.getTotalAssetAmount(), totalProfitLoss, totalReturnRate,
                stockReturnRate, etfReturnRate
        );

        List<PortfolioRecommendationResponse> recommendationResponses = recommendations.stream()
                .map(PortfolioRecommendationResponse::from)
                .toList();

        return new PortfolioAnalysisResponse(
                account.getAccountId(),
                summaryResponse,
                allocation,
                PortfolioDiagnosisResponse.from(diagnosis),
                recommendationResponses,
                holdingResponses
        );
    }
}
