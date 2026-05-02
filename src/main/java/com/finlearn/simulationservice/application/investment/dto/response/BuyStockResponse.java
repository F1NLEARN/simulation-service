package com.finlearn.simulationservice.application.investment.dto.response;

import java.util.UUID;

public record BuyStockResponse(
        UUID accountId,
        String stockCode,
        String stockName,
        String tradeType,
        long quantity,
        long price,
        long totalAmount,
        long cashBalanceAfterTrade
) {
}
