package com.finlearn.simulationservice.application.investment.dto.response;

public record StockPriceResponse(
        String stockCode,
        long currentPrice
) {
}
