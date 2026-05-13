package com.finlearn.simulationservice.domain.investment.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockBoughtEvent(
        UUID accountId,
        UUID investorId,
        UUID seasonId,
        int seasonNumber,
        String instrumentCode,
        long quantity,
        long tradePrice,
        long totalTradeAmount,
        LocalDateTime tradeAt
) {
}
