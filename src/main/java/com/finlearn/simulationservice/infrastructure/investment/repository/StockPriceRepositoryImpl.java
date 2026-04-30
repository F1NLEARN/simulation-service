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
        // MVP 단계에서는 stock_items.current_price(DB 적재값)를 현재 시세 소스로 사용한다.
        // 향후 Redis 캐시/외부 실시간 시세 API로 전환 시 이 구현체만 교체하면 된다.
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }

        String normalizedSymbol = symbol.trim().toUpperCase();
        return stockItemRepository.findByStockCode(normalizedSymbol)
                .flatMap(stockItem -> Optional.ofNullable(stockItem.getCurrentPrice()));
    }
}
