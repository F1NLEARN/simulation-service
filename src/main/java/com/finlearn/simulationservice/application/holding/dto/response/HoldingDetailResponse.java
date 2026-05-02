package com.finlearn.simulationservice.application.holding.dto.response;

import com.finlearn.simulationservice.domain.holding.entity.Holding;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingDetailResponse(
        UUID holdingId,
        UUID accountId,
        UUID seasonId,
        int seasonNumber,
        String holdingName,
        String instrumentCode,
        long quantity,
        long averageBuyPrice,
        long currentPrice,
        long totalBuyAmount,
        long valuationAmount,
        long unrealizedProfitLoss,
        BigDecimal returnRate
) {
    public static HoldingDetailResponse from(Holding holding) {
        return new HoldingDetailResponse(
                holding.getHoldingId(),
                holding.getAccountId(),
                holding.getSeasonId(),
                holding.getSeasonNumber(),
                holding.getHoldingName(),
                holding.getInstrumentCode().getValue(),
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                holding.getCurrentPrice(),
                holding.getTotalBuyAmount(),
                holding.getValuationAmount(),
                holding.getUnrealizedProfitLoss(),
                holding.getReturnRate()
        );
    }
}
