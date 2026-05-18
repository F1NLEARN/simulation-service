package com.finlearn.simulationservice.infrastructure.investment.config;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "docker"})
@RequiredArgsConstructor
public class LocalStockMasterInitializer implements ApplicationRunner {

    private final StockItemRepository stockItemRepository;

    @Value("${stock-master.seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        seedIfMissing("삼성전자", "005930", StockAssetType.STOCK);
        seedIfMissing("KODEX 200", "069500", StockAssetType.ETF);
    }

    private void seedIfMissing(String stockName, String stockCode, StockAssetType assetType) {
        if (stockItemRepository.findByStockCode(stockCode).isPresent()) {
            return;
        }
        stockItemRepository.save(StockItem.create(stockName, stockCode, assetType));
    }
}
