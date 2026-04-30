package com.finlearn.simulationservice.application.investment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BuyStockResponse(
        UUID accountId,
        String stockCode,
        String stockName,
        String tradeType,
        long quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        BigDecimal cashBalanceAfterTrade
) {
}
