package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.dto.ResolvedStockPrice;
import java.util.Optional;

public interface StockPriceRepository {

    Optional<ResolvedStockPrice> findCurrentPriceWithSource(String instrumentCode);

    default Optional<Long> findCurrentPrice(String instrumentCode) {
        return findCurrentPriceWithSource(instrumentCode)
                .map(ResolvedStockPrice::currentPrice);
    }
}
