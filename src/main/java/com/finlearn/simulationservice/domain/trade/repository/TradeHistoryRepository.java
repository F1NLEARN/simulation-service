package com.finlearn.simulationservice.domain.trade.repository;

import com.finlearn.simulationservice.domain.trade.entity.TradeHistory;
import com.finlearn.simulationservice.domain.trade.enums.TradeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeHistoryRepository {

    TradeHistory save(TradeHistory tradeHistory);

    List<TradeHistory> findAllByAccountId(UUID accountId);

    List<TradeHistory> findAllByAccountIdAndSeasonId(UUID accountId, UUID seasonId);

    @Query("""
            SELECT t
            FROM TradeHistory t
            WHERE t.accountId = :accountId
              AND (:instrumentCode IS NULL OR t.instrumentCode = :instrumentCode)
              AND (:tradeType IS NULL OR t.tradeType = :tradeType)
            """)
    Page<TradeHistory> findByAccountIdWithFilters(
            @Param("accountId") UUID accountId,
            @Param("instrumentCode") String instrumentCode,
            @Param("tradeType") TradeType tradeType,
            Pageable pageable
    );
}
