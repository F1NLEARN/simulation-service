package com.finlearn.simulationservice.application.holding.dto.response;

import com.finlearn.simulationservice.domain.holding.entity.Holding;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingListResponse(
        UUID holdingId,
        String holdingName,
        String instrumentCode,
        long quantity,
        long averageBuyPrice,
        long currentPrice,
        long valuationAmount,
        long unrealizedProfitLoss,
        BigDecimal returnRate
) {
    public static HoldingListResponse from(Holding holding) {
        return new HoldingListResponse(
                holding.getHoldingId(),
                holding.getHoldingName(),
                holding.getInstrumentCode().getValue(),
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                holding.getCurrentPrice(),
                holding.getValuationAmount(),
                holding.getUnrealizedProfitLoss(),
                holding.getReturnRate()
        );
    }
}
