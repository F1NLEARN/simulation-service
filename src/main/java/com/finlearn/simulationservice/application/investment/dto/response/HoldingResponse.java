package com.finlearn.simulationservice.application.investment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingResponse(
        UUID holdingId,
        String stockCode,
        String stockName,
        long quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalPurchaseAmount,
        BigDecimal currentEvaluationAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate
) {
}
