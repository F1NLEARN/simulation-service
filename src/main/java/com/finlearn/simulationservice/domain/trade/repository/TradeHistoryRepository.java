package com.finlearn.simulationservice.domain.trade.repository;

import com.finlearn.simulationservice.domain.trade.entity.TradeHistory;

import java.util.List;
import java.util.UUID;

public interface TradeHistoryRepository {

    TradeHistory save(TradeHistory tradeHistory);

    List<TradeHistory> findAllByAccountId(UUID accountId);

    List<TradeHistory> findAllByAccountIdAndSeasonId(UUID accountId, UUID seasonId);
}