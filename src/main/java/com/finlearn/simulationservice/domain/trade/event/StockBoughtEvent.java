package com.finlearn.simulationservice.domain.trade.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockBoughtEvent(
        UUID accountId,
        UUID seasonId,
        int seasonNumber,
        String instrumentCode,
        long quantity,
        long tradePrice,
        long totalTradeAmount,
        long cashBalanceAfterTrade,
        LocalDateTime tradeAt
) {
}