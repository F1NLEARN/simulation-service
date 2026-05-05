package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.holding.entity.Holding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public record PortfolioAnalysisResponse(
        UUID accountId,
        long totalBuyAmount,
        long totalValuationAmount,
        long totalProfitLoss,
        BigDecimal totalReturnRate,
        List<PortfolioHoldingResponse> holdings
) {
    public static PortfolioAnalysisResponse of(UUID accountId, List<Holding> holdings) {
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

        return new PortfolioAnalysisResponse(
                accountId,
                totalBuyAmount,
                totalValuationAmount,
                totalProfitLoss,
                totalReturnRate,
                holdingResponses
        );
    }
}
