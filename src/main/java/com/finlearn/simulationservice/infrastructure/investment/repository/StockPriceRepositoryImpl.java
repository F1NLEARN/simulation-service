package com.finlearn.simulationservice.infrastructure.investment.repository;

import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StockPriceRepositoryImpl implements StockPriceRepository {

    private final StockItemRepository stockItemRepository;

    @Override
    public Optional<BigDecimal> findCurrentPrice(StockAssetType assetType, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }

        String normalizedSymbol = symbol.trim().toUpperCase();
        return stockItemRepository.findByStockCode(normalizedSymbol)
                .flatMap(stockItem -> Optional.ofNullable(stockItem.getCurrentPrice()));
    }
}
