package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.math.BigDecimal;
import java.util.Optional;

public interface StockPriceRepository {

    Optional<BigDecimal> findCurrentPrice(StockAssetType assetType, String symbol);
}
