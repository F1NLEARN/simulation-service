package com.finlearn.simulationservice.application.tradehistory.dto.response;

import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradeHistoryDetailResponse(
        UUID tradeHistoryId,
        UUID accountId,
        UUID seasonId,
        int seasonNumber,
        String instrumentCode,
        TradeType tradeType,
        TradeStatus status,
        long quantity,
        long tradePrice,
        long totalTradeAmount,
        long cashBalanceAfterTrade,
        LocalDateTime tradeAt
) {
    public static TradeHistoryDetailResponse from(TradeHistory tradeHistory) {
        return new TradeHistoryDetailResponse(
                tradeHistory.getTradeHistoryId(),
                tradeHistory.getAccountId(),
                tradeHistory.getSeasonId(),
                tradeHistory.getSeasonNumber(),
                tradeHistory.getInstrumentCode().getValue(),
                tradeHistory.getTradeType(),
                tradeHistory.getStatus(),
                tradeHistory.getQuantity(),
                tradeHistory.getTradePrice(),
                tradeHistory.getTotalTradeAmount(),
                tradeHistory.getCashBalanceAfterTrade(),
                tradeHistory.getTradeAt()
        );
    }
}