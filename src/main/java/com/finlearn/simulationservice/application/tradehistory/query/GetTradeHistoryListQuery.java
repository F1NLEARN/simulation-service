package com.finlearn.simulationservice.application.tradehistory.query;

import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetTradeHistoryListQuery(
        UUID accountId,
        TradeType tradeType,
        TradeStatus status,
        String instrumentCode,
        Pageable pageable
) {
}