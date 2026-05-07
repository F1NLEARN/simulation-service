package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.holding.entity.Holding;

import java.math.BigDecimal;
import java.util.UUID;

public record PortfolioHoldingResponse(
        UUID holdingId,
        String instrumentCode,
        String holdingName,
        long quantity,
        long averageBuyPrice,
        long currentPrice,
        long totalBuyAmount,
        long valuationAmount,
        long unrealizedProfitLoss,
        BigDecimal returnRate,
        BigDecimal weight
) {
    public static PortfolioHoldingResponse from(Holding holding, BigDecimal weight) {
        return new PortfolioHoldingResponse(
                holding.getHoldingId(),
                holding.getInstrumentCode().getValue(),
                holding.getHoldingName(),
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                holding.getCurrentPrice(),
                holding.getTotalBuyAmount(),
                holding.getValuationAmount(),
                holding.getUnrealizedProfitLoss(),
                holding.getReturnRate(),
                weight
        );
    }
}
