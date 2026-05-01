package com.finlearn.simulationservice.infrastructure.tradehistory.persistence;

import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TradeHistoryJpaRepository extends JpaRepository<TradeHistory, UUID>, TradeHistoryRepository {

    @Override
    Optional<TradeHistory> findById(UUID tradeHistoryId);

    @Override
    @Query("SELECT t FROM TradeHistory t " +
            "WHERE t.accountId = :accountId " +
            "AND (:tradeType IS NULL OR t.tradeType = :tradeType) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:instrumentCode IS NULL OR t.instrumentCode.value = :instrumentCode)")
    Page<TradeHistory> findAllWithFilters(
            @Param("accountId") UUID accountId,
            @Param("tradeType") TradeType tradeType,
            @Param("status") TradeStatus status,
            @Param("instrumentCode") String instrumentCode,
            Pageable pageable
    );
}