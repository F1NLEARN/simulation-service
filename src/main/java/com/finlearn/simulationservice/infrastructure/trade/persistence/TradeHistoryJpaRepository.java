package com.finlearn.simulationservice.infrastructure.trade.persistence;

import com.finlearn.simulationservice.domain.trade.entity.TradeHistory;
import com.finlearn.simulationservice.domain.trade.repository.TradeHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TradeHistoryJpaRepository extends JpaRepository<TradeHistory, UUID>, TradeHistoryRepository {

    @Override
    List<TradeHistory> findAllByAccountId(UUID accountId);

    @Override
    List<TradeHistory> findAllByAccountIdAndSeasonId(UUID accountId, UUID seasonId);
}