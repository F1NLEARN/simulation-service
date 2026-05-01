package com.finlearn.simulationservice.application.tradehistory.dto.response;

import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradeHistoryListResponse(
        UUID tradeHistoryId,
        String instrumentCode,
        TradeType tradeType,
        TradeStatus status,
        long quantity,
        long tradePrice,
        long totalTradeAmount,
        LocalDateTime tradeAt
) {
    public static TradeHistoryListResponse from(TradeHistory tradeHistory) {
        return new TradeHistoryListResponse(
                tradeHistory.getTradeHistoryId(),
                tradeHistory.getInstrumentCode().getValue(),
                tradeHistory.getTradeType(),
                tradeHistory.getStatus(),
                tradeHistory.getQuantity(),
                tradeHistory.getTradePrice(),
                tradeHistory.getTotalTradeAmount(),
                tradeHistory.getTradeAt()
        );
    }
}