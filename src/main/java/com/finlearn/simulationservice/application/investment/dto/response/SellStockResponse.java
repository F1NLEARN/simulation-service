package com.finlearn.simulationservice.application.investment.dto.response;

import java.math.BigDecimal;

public record SellStockResponse(
        String stockCode,
        String stockName,
        long sellQuantity,
        BigDecimal sellPrice,
        BigDecimal totalSellAmount,
        long remainingQuantity,
        BigDecimal cashBalance
) {
}
