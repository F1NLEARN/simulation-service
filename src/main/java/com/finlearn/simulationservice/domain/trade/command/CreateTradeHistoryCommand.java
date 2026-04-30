package com.finlearn.simulationservice.domain.trade.command;

import com.finlearn.simulationservice.domain.trade.enums.TradeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTradeHistoryCommand(
        UUID accountId,
        UUID seasonId,
        int seasonNumber,
        String instrumentCode,
        TradeType tradeType,
        long quantity,
        long tradePrice,
        LocalDateTime tradeAt,
        long cashBalanceAfterTrade
) {
}