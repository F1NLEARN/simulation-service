package com.finlearn.simulationservice.application.investment.dto.response;

public record SellStockResponse(
        String stockCode,
        String stockName,
        long sellQuantity,
        long sellPrice,
        long totalSellAmount,
        long remainingQuantity,
        long cashBalance
) {
}
