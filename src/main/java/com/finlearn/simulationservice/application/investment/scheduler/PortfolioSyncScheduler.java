package com.finlearn.simulationservice.application.investment.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.domain.investment.entity.HoldingStock;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioSyncScheduler {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockItemRepository stockItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${portfolio-sync-scheduler.interval-ms:300000}")
    @Transactional
    public void syncPortfolioSnapshots() {
        List<InvestmentAccount> accounts = investmentAccountRepository.findAllByStatus(InvestmentAccountStatus.ACTIVE);
        if (accounts.isEmpty()) {
            return;
        }

        List<String> allCodes = accounts.stream()
                .flatMap(a -> a.getHoldingStocks().stream())
                .map(HoldingStock::getInstrumentCode)
                .distinct()
                .toList();

        if (allCodes.isEmpty()) {
            return;
        }

        Map<String, StockItem> stockItemMap = stockItemRepository.findAllByStockCodeIn(allCodes).stream()
                .collect(Collectors.toMap(StockItem::getStockCode, s -> s));

        log.info("[PortfolioSyncScheduler] 포트폴리오 스냅샷 동기화 시작 - 계좌 수: {}", accounts.size());
        int published = 0;

        for (InvestmentAccount account : accounts) {
            List<HoldingStock> holdings = account.getHoldingStocks();
            if (holdings.isEmpty()) {
                continue;
            }

            BigDecimal overallRate = computeReturnRate(holdings, stockItemMap, null);
            BigDecimal stockRate  = computeReturnRate(holdings, stockItemMap, StockAssetType.STOCK);
            BigDecimal etfRate    = computeReturnRate(holdings, stockItemMap, StockAssetType.ETF);

            int stockCount = (int) holdings.stream()
                    .filter(h -> assetType(stockItemMap, h) == StockAssetType.STOCK).count();
            int etfCount = (int) holdings.stream()
                    .filter(h -> assetType(stockItemMap, h) == StockAssetType.ETF).count();

            PortfolioSnapshotEvent event = new PortfolioSnapshotEvent(
                    account.getParticipant().getInvestorId(),
                    account.getAccountId(),
                    account.getParticipant().getSeasonId(),
                    account.getParticipant().getSeasonNumber(),
                    account.getParticipant().getInvestorName(),
                    overallRate, stockRate, etfRate,
                    stockCount, etfCount,
                    LocalDateTime.now()
            );

            try {
                outboxEventRepository.save(OutboxEvent.of(KafkaTopics.PORTFOLIO_SNAPSHOT, objectMapper.writeValueAsString(event)));
                published++;
            } catch (JsonProcessingException e) {
                log.warn("[PortfolioSyncScheduler] 직렬화 실패 - accountId={}", account.getAccountId());
            }
        }

        log.info("[PortfolioSyncScheduler] 포트폴리오 스냅샷 동기화 완료 - 발행: {}/{}", published, accounts.size());
    }

    private BigDecimal computeReturnRate(List<HoldingStock> holdings,
                                          Map<String, StockItem> stockItemMap,
                                          StockAssetType targetType) {
        List<HoldingStock> filtered = targetType == null ? holdings : holdings.stream()
                .filter(h -> assetType(stockItemMap, h) == targetType)
                .toList();

        long totalBuyAmount = filtered.stream().mapToLong(HoldingStock::getTotalBuyAmount).sum();
        if (totalBuyAmount == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long totalValuation = filtered.stream()
                .mapToLong(h -> {
                    StockItem item = stockItemMap.get(h.getInstrumentCode());
                    long price = (item != null && item.getCurrentPrice() != null && item.getCurrentPrice() > 0)
                            ? item.getCurrentPrice()
                            : h.getAverageBuyPrice();
                    return price * h.getQuantity();
                })
                .sum();

        long unrealizedPnL = totalValuation - totalBuyAmount;
        return BigDecimal.valueOf(unrealizedPnL)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBuyAmount), 2, RoundingMode.HALF_UP);
    }

    private StockAssetType assetType(Map<String, StockItem> stockItemMap, HoldingStock h) {
        StockItem item = stockItemMap.get(h.getInstrumentCode());
        return item != null ? item.getAssetType() : StockAssetType.STOCK;
    }
}