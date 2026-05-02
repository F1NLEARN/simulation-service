package com.finlearn.simulationservice.application.investment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TradeHistoryResponse(
        UUID tradeHistoryId,
        String stockCode,
        String tradeType,
        long quantity,
        BigDecimal tradePrice,
        BigDecimal totalTradeAmount,
        BigDecimal cashBalanceAfterTrade,
        LocalDateTime tradeAt
) {
}
