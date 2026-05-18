package com.finlearn.simulationservice.application.investment.service;

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
import com.finlearn.simulationservice.domain.tradehistory.event.TradeCompletedEvent;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentOrderServiceTest {

    @Mock private InvestmentAccountRepository investmentAccountRepository;
    @Mock private StockItemRepository stockItemRepository;
    @Mock private StockPriceRepository stockPriceRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private TradeHistoryRepository tradeHistoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InvestmentOrderService investmentOrderService;

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
        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(USER_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode(stockItem.getStockCode())).thenReturn(Optional.of(stockItem));
        when(stockPriceRepository.findCurrentPriceWithSource(stockItem.getStockCode()))
                .thenReturn(Optional.of(new ResolvedStockPrice(stockItem.getStockCode(), price, StockPriceSource.DB_CACHE)));
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, stockItem.getStockCode()))
                .thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void mockSellSetup(InvestmentAccount account, StockItem stockItem, Holding existingHolding, long price) {
        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(USER_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode(stockItem.getStockCode())).thenReturn(Optional.of(stockItem));
        when(stockPriceRepository.findCurrentPriceWithSource(stockItem.getStockCode()))
                .thenReturn(Optional.of(new ResolvedStockPrice(stockItem.getStockCode(), price, StockPriceSource.DB_CACHE)));
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, stockItem.getStockCode()))
                .thenReturn(Optional.of(existingHolding));
        when(tradeHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== TradeCompletedEvent 발행 검증 =====

    @Test
    @DisplayName("매수 체결 후 TradeCompletedEvent에 BUY, STOCK, userId, accountId, seasonId가 포함된다.")
    void buy_publishesTradeCompletedEventWithCorrectFields() {
        InvestmentAccount account = createActiveAccount();
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);
        mockBuySetup(account, stockItem, PRICE);

        investmentOrderService.buy(USER_ID, new BuyOrderRequest(STOCK_CODE, 10));

        ArgumentCaptor<TradeCompletedEvent> captor = ArgumentCaptor.forClass(TradeCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        TradeCompletedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(event.seasonId()).isEqualTo(SEASON_ID);
        assertThat(event.tradeType()).isEqualTo("BUY");
        assertThat(event.assetType()).isEqualTo("STOCK");
        assertThat(event.stockCode()).isEqualTo(STOCK_CODE);
        assertThat(event.executedAt()).isNotNull();
    }

    @Test
    @DisplayName("매도 체결 후 TradeCompletedEvent에 SELL이 포함된다.")
    void sell_publishesTradeCompletedEventWithSellType() {
        InvestmentAccount account = createActiveAccount();
        account.buy(STOCK_CODE, "삼성전자", 10, PRICE, null);
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);
        Holding existingHolding = Holding.create(new CreateHoldingCommand(
                ACCOUNT_ID, "삼성전자", SEASON_ID, 1, STOCK_CODE, 10L, PRICE, PRICE));
        mockSellSetup(account, stockItem, existingHolding, PRICE);

        investmentOrderService.sell(USER_ID, new SellOrderRequest(STOCK_CODE, 10));

        ArgumentCaptor<TradeCompletedEvent> captor = ArgumentCaptor.forClass(TradeCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        TradeCompletedEvent event = captor.getValue();
        assertThat(event.tradeType()).isEqualTo("SELL");
        assertThat(event.assetType()).isEqualTo("STOCK");
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.accountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    @DisplayName("ETF 매수 후 TradeCompletedEvent의 assetType이 ETF이다.")
    void buy_etf_publishesTradeCompletedEventWithEtfAssetType() {
        InvestmentAccount account = createActiveAccount();
        StockItem etfItem = StockItem.create("KODEX200", ETF_CODE, StockAssetType.ETF, PRICE);
        mockBuySetup(account, etfItem, PRICE);

        investmentOrderService.buy(USER_ID, new BuyOrderRequest(ETF_CODE, 5));

        ArgumentCaptor<TradeCompletedEvent> captor = ArgumentCaptor.forClass(TradeCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().assetType()).isEqualTo("ETF");
    }

    @Test
    @DisplayName("종목 코드는 대문자 정규화 후 TradeCompletedEvent에 담긴다.")
    void buy_normalizedStockCodeInEvent() {
        InvestmentAccount account = createActiveAccount();
        StockItem stockItem = StockItem.create("삼성전자", STOCK_CODE, StockAssetType.STOCK, PRICE);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(USER_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode(STOCK_CODE)).thenReturn(Optional.of(stockItem));
        when(stockPriceRepository.findCurrentPriceWithSource(STOCK_CODE))
                .thenReturn(Optional.of(new ResolvedStockPrice(STOCK_CODE, PRICE, StockPriceSource.DB_CACHE)));
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, STOCK_CODE))
                .thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        investmentOrderService.buy(USER_ID, new BuyOrderRequest("  005930  ", 10));

        ArgumentCaptor<TradeCompletedEvent> captor = ArgumentCaptor.forClass(TradeCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().stockCode()).isEqualTo(STOCK_CODE);
    }
}