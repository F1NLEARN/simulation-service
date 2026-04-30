package com.finlearn.simulationservice.domain.trade.repository;

import com.finlearn.simulationservice.domain.trade.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, UUID> {

    List<TradeHistory> findAllByAccountId(UUID accountId);

    List<TradeHistory> findAllByAccountIdAndSeasonId(UUID accountId, UUID seasonId);
}