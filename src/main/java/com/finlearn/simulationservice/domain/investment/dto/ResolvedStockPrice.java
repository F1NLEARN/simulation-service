package com.finlearn.simulationservice.domain.investment.dto;

import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;

public record ResolvedStockPrice(
        String stockCode,
        long currentPrice,
        StockPriceSource source
) {
}
