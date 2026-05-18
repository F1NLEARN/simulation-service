package com.finlearn.simulationservice.application.tradehistory.service;

import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.tradehistory.event.TradeCompletedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.producer.SimulationKafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeKafkaEventListenerTest {

    @Mock private HoldingRepository holdingRepository;
    @Mock private StockItemRepository stockItemRepository;
    @Mock private SimulationKafkaProducer kafkaProducer;

    @InjectMocks
    private TradeKafkaEventListener listener;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final String STOCK_CODE = "005930";
    private static final String ETF_CODE = "069500";
    private static final LocalDateTime EXECUTED_AT = LocalDateTime.of(2026, 5, 17, 10, 30);

    private TradeCompletedEvent buyEvent(String assetType) {
        return new TradeCompletedEvent(USER_ID, ACCOUNT_ID, SEASON_ID, "BUY", assetType, STOCK_CODE, EXECUTED_AT);
    }

    private Holding createHolding(String code, long quantity, long avgBuyPrice, long currentPrice) {
        String name = code.equals(ETF_CODE) ? "KODEX200" : "삼성전자";
        return Holding.create(new CreateHoldingCommand(
                ACCOUNT_ID, name, SEASON_ID, 1, code, quantity, avgBuyPrice, currentPrice));
    }

    // ===== simulation.trade.executed =====

    @Test
    @DisplayName("trade.executed 이벤트에 TradeCompletedEvent의 모든 필드가 그대로 담긴다.")
    void onTradeCompleted_publishesTradeExecutedWithAllFields() {
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());

        listener.onTradeCompleted(buyEvent("STOCK"));

        ArgumentCaptor<TradeExecutedEvent> captor = ArgumentCaptor.forClass(TradeExecutedEvent.class);
        verify(kafkaProducer).sendTradeExecuted(captor.capture());

        TradeExecutedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(event.seasonId()).isEqualTo(SEASON_ID);
        assertThat(event.tradeType()).isEqualTo("BUY");
        assertThat(event.assetType()).isEqualTo("STOCK");
        assertThat(event.stockCode()).isEqualTo(STOCK_CODE);
        assertThat(event.executedAt()).isEqualTo(EXECUTED_AT);
    }

    // ===== simulation.portfolio.snapshot =====

    @Test
    @DisplayName("보유 종목이 없으면 snapshot의 모든 수익률이 0.0이고 보유 수가 0이다.")
    void onTradeCompleted_emptyHoldings_snapshotAllZero() {
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());

        listener.onTradeCompleted(buyEvent("STOCK"));

        ArgumentCaptor<PortfolioSnapshotEvent> captor = ArgumentCaptor.forClass(PortfolioSnapshotEvent.class);
        verify(kafkaProducer).sendPortfolioSnapshot(captor.capture());

        PortfolioSnapshotEvent snapshot = captor.getValue();
        assertThat(snapshot.overallReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(snapshot.stockReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(snapshot.etfReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(snapshot.stockHoldingCount()).isZero();
        assertThat(snapshot.etfHoldingCount()).isZero();
    }

    @Test
    @DisplayName("STOCK만 보유 시 stockReturnRate가 계산되고 etfReturnRate는 0.0이다.")
    void onTradeCompleted_onlyStockHolding_stockReturnRateCalculatedEtfZero() {
        // qty=10, avgBuy=50_000, current=60_000 → unrealized=100_000, buy=500_000 → 20.00%
        Holding stockHolding = createHolding(STOCK_CODE, 10L, 50_000L, 60_000L);
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK);

        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(stockHolding));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of(stockItem));

        listener.onTradeCompleted(buyEvent("STOCK"));

        ArgumentCaptor<PortfolioSnapshotEvent> captor = ArgumentCaptor.forClass(PortfolioSnapshotEvent.class);
        verify(kafkaProducer).sendPortfolioSnapshot(captor.capture());

        PortfolioSnapshotEvent snapshot = captor.getValue();
        assertThat(snapshot.stockReturnRate()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(snapshot.etfReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(snapshot.overallReturnRate()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(snapshot.stockHoldingCount()).isEqualTo(1);
        assertThat(snapshot.etfHoldingCount()).isZero();
    }

    @Test
    @DisplayName("ETF만 보유 시 etfReturnRate가 계산되고 stockReturnRate는 0.0이다.")
    void onTradeCompleted_onlyEtfHolding_etfReturnRateCalculatedStockZero() {
        // qty=5, avgBuy=100_000, current=90_000 → unrealized=-50_000, buy=500_000 → -10.00%
        Holding etfHolding = createHolding(ETF_CODE, 5L, 100_000L, 90_000L);
        StockItem etfItem = StockItem.create("KODEX200", ETF_CODE, StockAssetType.ETF);

        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(etfHolding));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of(etfItem));

        listener.onTradeCompleted(new TradeCompletedEvent(
                USER_ID, ACCOUNT_ID, SEASON_ID, "BUY", "ETF", ETF_CODE, EXECUTED_AT));

        ArgumentCaptor<PortfolioSnapshotEvent> captor = ArgumentCaptor.forClass(PortfolioSnapshotEvent.class);
        verify(kafkaProducer).sendPortfolioSnapshot(captor.capture());

        PortfolioSnapshotEvent snapshot = captor.getValue();
        assertThat(snapshot.etfReturnRate()).isEqualByComparingTo(new BigDecimal("-10.00"));
        assertThat(snapshot.stockReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(snapshot.etfHoldingCount()).isEqualTo(1);
        assertThat(snapshot.stockHoldingCount()).isZero();
    }

    @Test
    @DisplayName("STOCK/ETF 혼합 보유 시 자산별 수익률이 독립적으로 계산된다.")
    void onTradeCompleted_mixedHoldings_assetReturnRatesCalculatedIndependently() {
        // STOCK: unrealized=100_000, buy=500_000 → 20.00%
        Holding stockHolding = createHolding(STOCK_CODE, 10L, 50_000L, 60_000L);
        // ETF:   unrealized=-50_000, buy=500_000 → -10.00%
        Holding etfHolding = createHolding(ETF_CODE, 5L, 100_000L, 90_000L);
        // overall: unrealized=50_000, buy=1_000_000 → 5.00%

        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK);
        StockItem etfItem = StockItem.create("KODEX200", ETF_CODE, StockAssetType.ETF);

        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(stockHolding, etfHolding));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of(stockItem, etfItem));

        listener.onTradeCompleted(buyEvent("STOCK"));

        ArgumentCaptor<PortfolioSnapshotEvent> captor = ArgumentCaptor.forClass(PortfolioSnapshotEvent.class);
        verify(kafkaProducer).sendPortfolioSnapshot(captor.capture());

        PortfolioSnapshotEvent snapshot = captor.getValue();
        assertThat(snapshot.stockReturnRate()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(snapshot.etfReturnRate()).isEqualByComparingTo(new BigDecimal("-10.00"));
        assertThat(snapshot.overallReturnRate()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(snapshot.stockHoldingCount()).isEqualTo(1);
        assertThat(snapshot.etfHoldingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("snapshot의 userId, accountId, seasonId, updatedAt이 이벤트 값과 일치한다.")
    void onTradeCompleted_snapshotContainsEventIdentifiers() {
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());

        listener.onTradeCompleted(buyEvent("STOCK"));

        ArgumentCaptor<PortfolioSnapshotEvent> captor = ArgumentCaptor.forClass(PortfolioSnapshotEvent.class);
        verify(kafkaProducer).sendPortfolioSnapshot(captor.capture());

        PortfolioSnapshotEvent snapshot = captor.getValue();
        assertThat(snapshot.userId()).isEqualTo(USER_ID);
        assertThat(snapshot.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(snapshot.seasonId()).isEqualTo(SEASON_ID);
        assertThat(snapshot.updatedAt()).isEqualTo(EXECUTED_AT);
    }

    @Test
    @DisplayName("trade.executed와 portfolio.snapshot 두 이벤트가 모두 발행된다.")
    void onTradeCompleted_publishesBothKafkaEvents() {
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());

        listener.onTradeCompleted(buyEvent("STOCK"));

        verify(kafkaProducer).sendTradeExecuted(any(TradeExecutedEvent.class));
        verify(kafkaProducer).sendPortfolioSnapshot(any(PortfolioSnapshotEvent.class));
    }
}