package com.finlearn.simulationservice.infrastructure.investment.repository;

import com.finlearn.simulationservice.domain.investment.dto.ResolvedStockPrice;
import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import com.finlearn.simulationservice.infrastructure.investment.client.KisStockPriceClient;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StockPriceRepositoryImpl implements StockPriceRepository {

    private final StockItemRepository stockItemRepository;
    private final KisStockPriceClient kisStockPriceClient;

    @Override
    public Optional<ResolvedStockPrice> findCurrentPriceWithSource(String instrumentCode) {
        if (instrumentCode == null || instrumentCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = instrumentCode.trim().toUpperCase();

        Optional<Long> livePrice = kisStockPriceClient.findCurrentPrice(normalized);
        if (livePrice.isPresent()) {
            return Optional.of(new ResolvedStockPrice(normalized, livePrice.get(), StockPriceSource.KIS));
        }

        // KIS 설정이 없거나 외부 API 조회가 실패하면 MVP용 DB 적재값을 fallback으로 사용한다.
        return stockItemRepository.findByStockCode(normalized)
                .flatMap(item -> Optional.ofNullable(item.getCurrentPrice()))
                .map(price -> new ResolvedStockPrice(normalized, price, StockPriceSource.DB_FALLBACK));
    }
}
