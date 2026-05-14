package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import java.time.LocalDateTime;

public record StockPriceResponse(
        String stockCode,
        long currentPrice,
        StockPriceSource source,
        LocalDateTime cachedAt
) {
}
