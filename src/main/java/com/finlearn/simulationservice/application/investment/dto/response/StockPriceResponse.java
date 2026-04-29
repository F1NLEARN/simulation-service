package com.finlearn.simulationservice.application.investment.dto.response;

import java.math.BigDecimal;

public record StockPriceResponse(
        String stockCode,
        BigDecimal currentPrice
) {
}
