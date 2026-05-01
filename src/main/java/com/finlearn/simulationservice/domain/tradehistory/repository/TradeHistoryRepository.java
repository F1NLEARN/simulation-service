package com.finlearn.simulationservice.domain.tradehistory.repository;

import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface TradeHistoryRepository {

    TradeHistory save(TradeHistory tradeHistory);

    Optional<TradeHistory> findById(UUID tradeHistoryId);

    Page<TradeHistory> findAllWithFilters(UUID accountId, TradeType tradeType, TradeStatus status,
                                          String instrumentCode, Pageable pageable);
}