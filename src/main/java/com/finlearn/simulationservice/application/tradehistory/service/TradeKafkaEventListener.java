package com.finlearn.simulationservice.application.tradehistory.service;

import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.tradehistory.event.TradeCompletedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.producer.SimulationKafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(SimulationKafkaProducer.class)
public class TradeKafkaEventListener {

    private final HoldingRepository holdingRepository;
    private final StockItemRepository stockItemRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final SimulationKafkaProducer kafkaProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onTradeCompleted(TradeCompletedEvent event) {
        List<Holding> holdings = holdingRepository.findAllWithFilter(event.accountId(), null);

        InvestmentAccount account = investmentAccountRepository.findById(event.accountId()).orElse(null);
        int seasonNumber = account != null ? account.getParticipant().getSeasonNumber() : 0;
        String userNickname = account != null ? account.getParticipant().getInvestorName() : null;

        StockAssetType currentAssetType = StockAssetType.valueOf(event.assetType());
        Map<String, StockAssetType> assetTypeMap = holdings.isEmpty() ? Map.of() :
                stockItemRepository.findAllByStockCodeIn(
                        holdings.stream().map(h -> h.getInstrumentCode().getValue()).toList()
                ).stream().collect(Collectors.toMap(StockItem::getStockCode, StockItem::getAssetType));

        int holdCount = (int) holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), currentAssetType) == currentAssetType)
                .count();
        double returnRate = account != null ? account.getTotalReturnRate().doubleValue() : 0.0;

        kafkaProducer.sendTradeExecuted(new TradeExecutedEvent(
                event.userId(), event.accountId(), event.seasonId(), seasonNumber,
                event.tradeType(), event.assetType(), event.stockCode(),
                holdCount, returnRate, userNickname, event.executedAt()
        ));

        kafkaProducer.sendPortfolioSnapshot(buildSnapshotEvent(event, holdings));
    }

    private PortfolioSnapshotEvent buildSnapshotEvent(TradeCompletedEvent event, List<Holding> holdings) {
        Map<String, StockAssetType> assetTypeMap = holdings.isEmpty() ? Map.of() :
                stockItemRepository.findAllByStockCodeIn(
                        holdings.stream().map(h -> h.getInstrumentCode().getValue()).toList()
                ).stream().collect(Collectors.toMap(StockItem::getStockCode, StockItem::getAssetType));

        BigDecimal overallReturnRate = computeReturnRate(holdings, assetTypeMap, null);
        BigDecimal stockReturnRate = computeReturnRate(holdings, assetTypeMap, StockAssetType.STOCK);
        BigDecimal etfReturnRate = computeReturnRate(holdings, assetTypeMap, StockAssetType.ETF);

        int stockHoldingCount = (int) holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), StockAssetType.STOCK) == StockAssetType.STOCK)
                .count();
        int etfHoldingCount = (int) holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), StockAssetType.STOCK) == StockAssetType.ETF)
                .count();

        return new PortfolioSnapshotEvent(
                event.userId(), event.accountId(), event.seasonId(),
                overallReturnRate, stockReturnRate, etfReturnRate,
                stockHoldingCount, etfHoldingCount, event.executedAt()
        );
    }

    private BigDecimal computeReturnRate(List<Holding> holdings, Map<String, StockAssetType> assetTypeMap,
                                         StockAssetType targetType) {
        List<Holding> filtered = targetType == null ? holdings : holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), StockAssetType.STOCK) == targetType)
                .toList();

        long totalBuyAmount = filtered.stream().mapToLong(Holding::getTotalBuyAmount).sum();
        if (totalBuyAmount == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long totalUnrealizedProfitLoss = filtered.stream().mapToLong(Holding::getUnrealizedProfitLoss).sum();
        return BigDecimal.valueOf(totalUnrealizedProfitLoss)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBuyAmount), 2, RoundingMode.HALF_UP);
    }
}