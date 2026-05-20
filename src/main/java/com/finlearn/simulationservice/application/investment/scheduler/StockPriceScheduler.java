package com.finlearn.simulationservice.application.investment.scheduler;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.infrastructure.investment.client.KisStockPriceClient;
import com.finlearn.simulationservice.infrastructure.investment.config.KisApiProperties;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceScheduler {

    private final KisApiProperties kisApiProperties;
    private final KisStockPriceClient kisStockPriceClient;
    private final StockItemRepository stockItemRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refreshAllStockPrices();
    }

    @Scheduled(fixedRateString = "${stock-price-scheduler.interval-ms:600000}")
    @Transactional
    public void refreshAllStockPrices() {
        if (!kisApiProperties.isReady()) {
            return;
        }

        List<StockItem> stocks = stockItemRepository.findAllByOrderByStockCodeAsc();
        if (stocks.isEmpty()) {
            return;
        }

        log.info("[StockPriceScheduler] 전체 종목 가격 갱신 시작 - 종목 수: {}", stocks.size());
        int updatedCount = 0;

        for (StockItem stock : stocks) {
            try {
                Long price = kisStockPriceClient.findCurrentPrice(stock.getStockCode()).orElse(null);
                if (price != null && price > 0) {
                    stock.updateCurrentPrice(price, LocalDateTime.now());
                    updatedCount++;
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[StockPriceScheduler] 가격 갱신 인터럽트 - 중단");
                break;
            } catch (Exception e) {
                log.warn("[StockPriceScheduler] 가격 갱신 실패 - stockCode={}, reason={}", stock.getStockCode(), e.getMessage());
            }
        }

        log.info("[StockPriceScheduler] 전체 종목 가격 갱신 완료 - 갱신: {}/{}", updatedCount, stocks.size());
    }
}