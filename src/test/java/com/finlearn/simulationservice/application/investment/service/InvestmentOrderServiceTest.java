package com.finlearn.simulationservice.application.investment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finlearn.simulationservice.application.investment.dto.request.BuyOrderRequest;
import com.finlearn.simulationservice.application.investment.dto.request.SellOrderRequest;
import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.dto.ResolvedStockPrice;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentOrderServiceTest {

    @Mock private InvestmentAccountRepository investmentAccountRepository;
    @Mock private StockItemRepository stockItemRepository;
    @Mock private StockPriceRepository stockPriceRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private TradeHistoryRepository tradeHistoryRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private InvestmentOrderService investmentOrderService;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final String STOCK_CODE = "005930";
    private static final String ETF_CODE = "069500";
    private static final long PRICE = 50_000L;

    private InvestmentAccount createActiveAccount() {
        InvestmentAccount account = InvestmentAccount.open(
                new SeasonParticipant(USER_ID, "테스터", SEASON_ID, 1), 10_000_000L);
        ReflectionTestUtils.setField(account, "accountId", ACCOUNT_ID);
        return account;
    }

    private void mockBuySetup(InvestmentAccount account, StockItem stockItem, long price) {
        when(investmentAccountRepository.findByInvestorIdAndStatusForUpdate(USER_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode(stockItem.getStockCode())).thenReturn(Optional.of(stockItem));
        when(stockPriceRepository.findCurrentPriceWithSource(stockItem.getStockCode()))
                .thenReturn(Optional.of(new ResolvedStockPrice(stockItem.getStockCode(), price, StockPriceSource.DB_CACHE)));
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, stockItem.getStockCode()))
                .thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());
        when(tradeHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void mockSellSetup(InvestmentAccount account, StockItem stockItem, Holding existingHolding, long price) {
        when(investmentAccountRepository.findByInvestorIdAndStatusForUpdate(USER_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode(stockItem.getStockCode())).thenReturn(Optional.of(stockItem));
        when(stockPriceRepository.findCurrentPriceWithSource(stockItem.getStockCode()))
                .thenReturn(Optional.of(new ResolvedStockPrice(stockItem.getStockCode(), price, StockPriceSource.DB_CACHE)));
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, stockItem.getStockCode()))
                .thenReturn(Optional.of(existingHolding));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());
        when(tradeHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== Outbox 저장 검증 =====

    @Test
    @DisplayName("매수 체결 시 trade.executed, portfolio.snapshot 두 이벤트가 outbox에 저장된다.")
    void buy_savesTwoOutboxEvents() {
        InvestmentAccount account = createActiveAccount();
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);
        mockBuySetup(account, stockItem, PRICE);

        investmentOrderService.buy(USER_ID, new BuyOrderRequest(STOCK_CODE, 10));

        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("매도 체결 시 trade.executed, portfolio.snapshot 두 이벤트가 outbox에 저장된다.")
    void sell_savesTwoOutboxEvents() {
        InvestmentAccount account = createActiveAccount();
        account.buy(STOCK_CODE, "삼성전자", 10, PRICE, null);
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);
        Holding existingHolding = Holding.create(new CreateHoldingCommand(
                ACCOUNT_ID, "삼성전자", SEASON_ID, 1, STOCK_CODE, 10L, PRICE, PRICE));
        mockSellSetup(account, stockItem, existingHolding, PRICE);

        investmentOrderService.sell(USER_ID, new SellOrderRequest(STOCK_CODE, 10));

        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("저장된 Outbox 이벤트의 토픽이 각각 trade.executed, portfolio.snapshot이다.")
    void buy_outboxEventsHaveCorrectTopics() {
        InvestmentAccount account = createActiveAccount();
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);
        mockBuySetup(account, stockItem, PRICE);

        investmentOrderService.buy(USER_ID, new BuyOrderRequest(STOCK_CODE, 10));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(captor.capture());

        List<String> topics = captor.getAllValues().stream().map(OutboxEvent::getTopic).toList();
        assertThat(topics).containsExactlyInAnyOrder(
                KafkaTopics.TRADE_EXECUTED,
                KafkaTopics.PORTFOLIO_SNAPSHOT
        );
    }

    @Test
    @DisplayName("저장된 Outbox 이벤트의 초기 status가 PENDING이다.")
    void buy_outboxEventsHavePendingStatus() {
        InvestmentAccount account = createActiveAccount();
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);
        mockBuySetup(account, stockItem, PRICE);

        investmentOrderService.buy(USER_ID, new BuyOrderRequest(STOCK_CODE, 10));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .allMatch(e -> e.getStatus() == OutboxEventStatus.PENDING);
    }

    @Test
    @DisplayName("trade.executed payload에 BUY, STOCK, stockCode가 포함된다.")
    void buy_tradeExecutedPayloadContainsCorrectFields() throws Exception {
        InvestmentAccount account = createActiveAccount();
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);
        mockBuySetup(account, stockItem, PRICE);

        investmentOrderService.buy(USER_ID, new BuyOrderRequest(STOCK_CODE, 10));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(captor.capture());

        OutboxEvent tradeEvent = captor.getAllValues().stream()
                .filter(e -> KafkaTopics.TRADE_EXECUTED.equals(e.getTopic()))
                .findFirst().orElseThrow();

        String payload = tradeEvent.getPayload();
        assertThat(payload).contains("\"tradeType\":\"BUY\"");
        assertThat(payload).contains("\"assetType\":\"STOCK\"");
        assertThat(payload).contains("\"stockCode\":\"" + STOCK_CODE + "\"");
    }

    @Test
    @DisplayName("ETF 매수 시 trade.executed payload의 assetType이 ETF이다.")
    void buy_etf_tradeExecutedPayloadHasEtfAssetType() throws Exception {
        InvestmentAccount account = createActiveAccount();
        StockItem etfItem = StockItem.create("KODEX200", ETF_CODE, StockAssetType.ETF, PRICE);
        mockBuySetup(account, etfItem, PRICE);

        investmentOrderService.buy(USER_ID, new BuyOrderRequest(ETF_CODE, 5));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(captor.capture());

        OutboxEvent tradeEvent = captor.getAllValues().stream()
                .filter(e -> KafkaTopics.TRADE_EXECUTED.equals(e.getTopic()))
                .findFirst().orElseThrow();

        assertThat(tradeEvent.getPayload()).contains("\"assetType\":\"ETF\"");
    }
}