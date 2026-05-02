package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.request.BuyStockRequest;
import com.finlearn.simulationservice.application.investment.dto.request.RegisterFavoriteStockRequest;
import com.finlearn.simulationservice.application.investment.dto.request.SellStockRequest;
import com.finlearn.simulationservice.application.investment.dto.response.FavoriteStockResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockItemResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockPriceResponse;
import com.finlearn.simulationservice.domain.investment.entity.FavoriteStock;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.SeedMoneyGrantHistory;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.event.PointQuizPassedEvent;
import com.finlearn.simulationservice.domain.investment.event.SeasonInvestmentAccountOpenedEvent;
import com.finlearn.simulationservice.domain.investment.event.SeedMoneyGrantedEvent;
import com.finlearn.simulationservice.domain.investment.event.StockBoughtEvent;
import com.finlearn.simulationservice.domain.investment.event.StockSoldEvent;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.FavoriteStockRepository;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.SeedMoneyGrantHistoryRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceTest {

    @Mock
    private FavoriteStockRepository favoriteStockRepository;

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private SeedMoneyGrantHistoryRepository seedMoneyGrantHistoryRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private StockPriceRepository stockPriceRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InvestmentService investmentService;

    private static SeasonParticipant testParticipant(UUID investorId, UUID seasonId) {
        return new SeasonParticipant(investorId, "테스트유저", seasonId, 1);
    }

    // ===== handlePointQuizPassed =====

    @Test
    @DisplayName("포인트 퀴즈 통과 시 계좌를 생성하고 이벤트를 발행한다.")
    void handlePointQuizPassedCreateAccount() {
        UUID seasonId = UUID.randomUUID();
        UUID investorId = UUID.randomUUID();
        PointQuizPassedEvent event = new PointQuizPassedEvent(seasonId, 1, investorId, "테스트유저", 1000000L);

        InvestmentAccount savedAccount = InvestmentAccount.open(testParticipant(investorId, seasonId), 1000000L);
        ReflectionTestUtils.setField(savedAccount, "accountId", UUID.randomUUID());

        when(investmentAccountRepository.findByParticipant_InvestorIdAndParticipant_SeasonId(investorId, seasonId))
                .thenReturn(Optional.empty());
        when(investmentAccountRepository.save(any(InvestmentAccount.class))).thenReturn(savedAccount);

        investmentService.handlePointQuizPassed(event);

        verify(investmentAccountRepository, times(1)).save(any(InvestmentAccount.class));
        verify(seedMoneyGrantHistoryRepository, times(1)).save(any(SeedMoneyGrantHistory.class));
        verify(eventPublisher, times(1)).publishEvent(any(SeasonInvestmentAccountOpenedEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(SeedMoneyGrantedEvent.class));
    }

    @Test
    @DisplayName("계좌가 이미 있으면 아무것도 생성하지 않는다.")
    void handlePointQuizPassedSkipWhenAlreadyExists() {
        UUID seasonId = UUID.randomUUID();
        UUID investorId = UUID.randomUUID();
        PointQuizPassedEvent event = new PointQuizPassedEvent(seasonId, 1, investorId, "테스트유저", 1000000L);

        InvestmentAccount existingAccount = InvestmentAccount.open(testParticipant(investorId, seasonId), 1000000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndParticipant_SeasonId(investorId, seasonId))
                .thenReturn(Optional.of(existingAccount));

        investmentService.handlePointQuizPassed(event);

        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
        verify(seedMoneyGrantHistoryRepository, never()).save(any(SeedMoneyGrantHistory.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("계좌 생성 중 유니크 충돌이 발생하면 재조회로 복구하고 이벤트를 중복 발행하지 않는다.")
    void handlePointQuizPassedRecoverWhenAccountUniqueConflict() {
        UUID seasonId = UUID.randomUUID();
        UUID investorId = UUID.randomUUID();
        PointQuizPassedEvent event = new PointQuizPassedEvent(seasonId, 1, investorId, "테스트유저", 1000000L);

        InvestmentAccount existingAccount = InvestmentAccount.open(testParticipant(investorId, seasonId), 1000000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndParticipant_SeasonId(investorId, seasonId))
                .thenReturn(Optional.empty(), Optional.of(existingAccount));
        when(investmentAccountRepository.save(any(InvestmentAccount.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertDoesNotThrow(() -> investmentService.handlePointQuizPassed(event));

        verify(investmentAccountRepository, times(1)).save(any(InvestmentAccount.class));
        verify(seedMoneyGrantHistoryRepository, never()).save(any(SeedMoneyGrantHistory.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ===== buyStock =====

    @Test
    @DisplayName("매수 주문 성공 시 거래 이력 저장, 보유 종목 갱신, 예수금 차감 후 StockBought 이벤트를 발행한다.")
    void buyStockSuccess() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 100000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);

        BuyStockRequest request = new BuyStockRequest(accountId, "005930", 10);
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK, 5000L);

        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.of(stockItem));

        investmentService.buyStock(request);

        ArgumentCaptor<InvestmentAccount> accountCaptor = ArgumentCaptor.forClass(InvestmentAccount.class);
        verify(investmentAccountRepository, times(1)).save(accountCaptor.capture());
        verify(eventPublisher, times(1)).publishEvent(any(StockBoughtEvent.class));

        InvestmentAccount saved = accountCaptor.getValue();
        assertEquals(50000L, saved.getCurrentCashBalance());
        assertEquals(1, saved.getHoldingStocks().size());
        assertEquals(1, saved.getStockTransactions().size());
    }

    @Test
    @DisplayName("계좌가 ACTIVE가 아니면 매수에 실패한다.")
    void buyStockFailWhenAccountNotActive() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 100000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);
        account.close();

        BuyStockRequest request = new BuyStockRequest(accountId, "005930", 1);
        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        InvestmentException exception = assertThrows(InvestmentException.class, () -> investmentService.buyStock(request));

        assertEquals(InvestmentErrorCode.INVALID_ACCOUNT_STATUS, exception.getErrorCode());
        verify(stockItemRepository, never()).findByStockCode(any());
        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("현재 시세가 없으면 매수에 실패한다.")
    void buyStockFailWhenStockPriceNotFound() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 100000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);
        BuyStockRequest request = new BuyStockRequest(accountId, "005930", 1);

        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.empty());

        InvestmentException exception = assertThrows(InvestmentException.class, () -> investmentService.buyStock(request));

        assertEquals(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND, exception.getErrorCode());
        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("예수금이 부족하면 매수에 실패한다.")
    void buyStockFailWhenCashInsufficient() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 1000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);
        BuyStockRequest request = new BuyStockRequest(accountId, "005930", 2);
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK, 1000L);

        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.of(stockItem));

        InvestmentException exception = assertThrows(InvestmentException.class, () -> investmentService.buyStock(request));

        assertEquals(InvestmentErrorCode.INSUFFICIENT_CASH, exception.getErrorCode());
        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ===== sellStock =====

    @Test
    @DisplayName("매도 주문 성공 시 거래 이력 저장, 보유 종목 수량 차감, 예수금 증가 후 StockSold 이벤트를 발행한다.")
    void sellStockSuccess() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 100000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);
        account.buy("005930", "삼성전자", 10, 5000L, null);

        SellStockRequest request = new SellStockRequest(accountId, "005930", 10);

        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(stockPriceRepository.findCurrentPrice("005930")).thenReturn(Optional.of(6000L));

        investmentService.sellStock(request);

        ArgumentCaptor<InvestmentAccount> accountCaptor = ArgumentCaptor.forClass(InvestmentAccount.class);
        verify(investmentAccountRepository, times(1)).save(accountCaptor.capture());
        verify(eventPublisher, times(1)).publishEvent(any(StockSoldEvent.class));

        InvestmentAccount saved = accountCaptor.getValue();
        assertEquals(110000L, saved.getCurrentCashBalance());
        assertEquals(0, saved.getHoldingStocks().size());
        assertEquals(2, saved.getStockTransactions().size());
    }

    @Test
    @DisplayName("계좌가 ACTIVE가 아니면 매도에 실패한다.")
    void sellStockFailWhenAccountNotActive() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 100000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);
        account.close();

        SellStockRequest request = new SellStockRequest(accountId, "005930", 1);
        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        InvestmentException exception = assertThrows(InvestmentException.class, () -> investmentService.sellStock(request));

        assertEquals(InvestmentErrorCode.INVALID_ACCOUNT_STATUS, exception.getErrorCode());
        verify(stockPriceRepository, never()).findCurrentPrice(any());
        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("현재 시세가 없으면 매도에 실패한다.")
    void sellStockFailWhenStockPriceNotFound() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 100000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);
        SellStockRequest request = new SellStockRequest(accountId, "005930", 1);

        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(stockPriceRepository.findCurrentPrice("005930")).thenReturn(Optional.empty());

        InvestmentException exception = assertThrows(InvestmentException.class, () -> investmentService.sellStock(request));

        assertEquals(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND, exception.getErrorCode());
        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("보유 수량이 부족하면 매도에 실패한다.")
    void sellStockFailWhenInsufficientHoldingQuantity() {
        UUID accountId = UUID.randomUUID();
        InvestmentAccount account = InvestmentAccount.open(
                testParticipant(UUID.randomUUID(), UUID.randomUUID()), 100000L);
        ReflectionTestUtils.setField(account, "accountId", accountId);
        account.buy("005930", "삼성전자", 1, 5000L, null);

        SellStockRequest request = new SellStockRequest(accountId, "005930", 2);
        when(investmentAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(stockPriceRepository.findCurrentPrice("005930")).thenReturn(Optional.of(6000L));

        InvestmentException exception = assertThrows(InvestmentException.class, () -> investmentService.sellStock(request));

        assertEquals(InvestmentErrorCode.INSUFFICIENT_HOLDING_QUANTITY, exception.getErrorCode());
        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ===== FavoriteStock =====

    @Test
    @DisplayName("관심 종목 등록 성공 시 아이디를 반환한다.")
    void registerFavoriteStockSuccess() {
        UUID userId = UUID.randomUUID();
        RegisterFavoriteStockRequest request = new RegisterFavoriteStockRequest(StockAssetType.STOCK, "005930");
        FavoriteStock saved = FavoriteStock.register(userId, StockAssetType.STOCK, "005930");
        UUID favoriteStockId = UUID.randomUUID();
        ReflectionTestUtils.setField(saved, "favoriteStockId", favoriteStockId);

        when(favoriteStockRepository.existsByUserIdAndSymbol(userId, "005930")).thenReturn(false);
        when(favoriteStockRepository.save(any(FavoriteStock.class))).thenReturn(saved);

        UUID result = investmentService.registerFavoriteStock(userId, request);

        assertEquals(favoriteStockId, result);
        verify(favoriteStockRepository, times(1)).save(any(FavoriteStock.class));
    }

    @Test
    @DisplayName("관심 종목 심볼은 trim/uppercase 정규화 후 저장된다.")
    void registerFavoriteStockNormalizesSymbol() {
        UUID userId = UUID.randomUUID();
        RegisterFavoriteStockRequest request = new RegisterFavoriteStockRequest(StockAssetType.STOCK, "  aBc123  ");
        FavoriteStock saved = FavoriteStock.register(userId, StockAssetType.STOCK, "ABC123");
        UUID favoriteStockId = UUID.randomUUID();
        ReflectionTestUtils.setField(saved, "favoriteStockId", favoriteStockId);

        when(favoriteStockRepository.existsByUserIdAndSymbol(userId, "ABC123")).thenReturn(false);
        when(favoriteStockRepository.save(any(FavoriteStock.class))).thenReturn(saved);

        UUID result = investmentService.registerFavoriteStock(userId, request);

        assertEquals(favoriteStockId, result);
        verify(favoriteStockRepository, times(1)).existsByUserIdAndSymbol(userId, "ABC123");
    }

    @Test
    @DisplayName("동일 유저/동일 종목은 중복 등록할 수 없다.")
    void registerFavoriteStockFailWhenDuplicate() {
        UUID userId = UUID.randomUUID();
        RegisterFavoriteStockRequest request = new RegisterFavoriteStockRequest(StockAssetType.STOCK, "005930");
        when(favoriteStockRepository.existsByUserIdAndSymbol(userId, "005930")).thenReturn(true);

        InvestmentException exception = assertThrows(
                InvestmentException.class,
                () -> investmentService.registerFavoriteStock(userId, request)
        );

        assertEquals(InvestmentErrorCode.FAVORITE_STOCK_ALREADY_EXISTS, exception.getErrorCode());
        verify(favoriteStockRepository, never()).save(any(FavoriteStock.class));
    }

    @Test
    @DisplayName("동시 등록으로 DB 유니크 제약이 발생해도 중복 예외로 변환된다.")
    void registerFavoriteStockFailWhenDuplicateByUniqueConstraint() {
        UUID userId = UUID.randomUUID();
        RegisterFavoriteStockRequest request = new RegisterFavoriteStockRequest(StockAssetType.STOCK, "005930");
        when(favoriteStockRepository.existsByUserIdAndSymbol(userId, "005930")).thenReturn(false);
        when(favoriteStockRepository.save(any(FavoriteStock.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        InvestmentException exception = assertThrows(
                InvestmentException.class,
                () -> investmentService.registerFavoriteStock(userId, request)
        );

        assertEquals(InvestmentErrorCode.FAVORITE_STOCK_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("관심 종목 조회는 유저 기준으로 반환된다.")
    void getFavoriteStocksByUser() {
        UUID userId = UUID.randomUUID();
        FavoriteStock first = FavoriteStock.register(userId, StockAssetType.STOCK, "005930");
        FavoriteStock second = FavoriteStock.register(userId, StockAssetType.ETF, "069500");
        StockItem firstItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK);
        StockItem secondItem = StockItem.create("KODEX 200", "069500", StockAssetType.ETF);
        ReflectionTestUtils.setField(first, "favoriteStockId", UUID.randomUUID());
        ReflectionTestUtils.setField(second, "favoriteStockId", UUID.randomUUID());

        when(favoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(first, second));
        when(stockItemRepository.findAllByStockCodeIn(List.of("005930", "069500")))
                .thenReturn(List.of(firstItem, secondItem));

        List<FavoriteStockResponse> result = investmentService.getFavoriteStocks(userId);

        assertEquals(2, result.size());
        assertEquals("005930", result.get(0).symbol());
        assertEquals("삼성전자", result.get(0).stockName());
        assertEquals("069500", result.get(1).symbol());
        assertEquals("KODEX 200", result.get(1).stockName());
    }

    @Test
    @DisplayName("종목 정보가 없으면 관심 종목명은 symbol 값으로 fallback 된다.")
    void getFavoriteStocksFallbackToSymbolWhenStockNameMissing() {
        UUID userId = UUID.randomUUID();
        FavoriteStock favoriteStock = FavoriteStock.register(userId, StockAssetType.STOCK, "MISSING01");
        ReflectionTestUtils.setField(favoriteStock, "favoriteStockId", UUID.randomUUID());

        when(favoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(favoriteStock));
        when(stockItemRepository.findAllByStockCodeIn(List.of("MISSING01"))).thenReturn(List.of());

        List<FavoriteStockResponse> result = investmentService.getFavoriteStocks(userId);

        assertEquals(1, result.size());
        assertEquals("MISSING01", result.get(0).symbol());
        assertEquals("MISSING01", result.get(0).stockName());
    }

    @Test
    @DisplayName("관심 종목 삭제 대상이 없으면 예외가 발생한다.")
    void deleteFavoriteStockFailWhenTargetNotFound() {
        UUID userId = UUID.randomUUID();
        when(favoriteStockRepository.findByUserIdAndSymbol(userId, "005930")).thenReturn(Optional.empty());

        InvestmentException exception = assertThrows(
                InvestmentException.class,
                () -> investmentService.deleteFavoriteStock(userId, "005930")
        );

        assertEquals(InvestmentErrorCode.FAVORITE_STOCK_NOT_FOUND, exception.getErrorCode());
        verify(favoriteStockRepository, never()).delete(any(FavoriteStock.class));
    }

    @Test
    @DisplayName("관심 종목 삭제 성공 시 엔티티를 삭제한다.")
    void deleteFavoriteStockSuccess() {
        UUID userId = UUID.randomUUID();
        FavoriteStock favoriteStock = FavoriteStock.register(userId, StockAssetType.STOCK, "005930");
        when(favoriteStockRepository.findByUserIdAndSymbol(userId, "005930")).thenReturn(Optional.of(favoriteStock));

        investmentService.deleteFavoriteStock(userId, "005930");

        verify(favoriteStockRepository, times(1)).delete(favoriteStock);
    }

    @Test
    @DisplayName("삭제 시 심볼은 trim/uppercase 정규화 후 조회한다.")
    void deleteFavoriteStockNormalizesSymbol() {
        UUID userId = UUID.randomUUID();
        FavoriteStock favoriteStock = FavoriteStock.register(userId, StockAssetType.STOCK, "ABC123");
        when(favoriteStockRepository.findByUserIdAndSymbol(userId, "ABC123")).thenReturn(Optional.of(favoriteStock));

        investmentService.deleteFavoriteStock(userId, "  abc123 ");

        verify(favoriteStockRepository, times(1)).findByUserIdAndSymbol(userId, "ABC123");
        verify(favoriteStockRepository, times(1)).delete(favoriteStock);
    }

    @Test
    @DisplayName("자산유형 필터가 없으면 STOCK/ETF 전체 종목을 조회한다.")
    void getStockItemsWithoutAssetType() {
        StockItem first = StockItem.create("삼성전자", "005930", StockAssetType.STOCK);
        StockItem second = StockItem.create("KODEX 200", "069500", StockAssetType.ETF);
        when(stockItemRepository.findAllByCurrentPriceIsNotNullOrderByStockCodeAsc()).thenReturn(List.of(first, second));

        List<StockItemResponse> result = investmentService.getStockItems(null);

        assertEquals(2, result.size());
        assertEquals("삼성전자", result.get(0).name());
        assertEquals("005930", result.get(0).stockCode());
        assertEquals(StockAssetType.ETF, result.get(1).assetType());
    }

    @Test
    @DisplayName("자산유형 필터가 있으면 해당 유형 종목만 조회한다.")
    void getStockItemsWithAssetType() {
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK);
        when(stockItemRepository.findAllByAssetTypeAndCurrentPriceIsNotNullOrderByStockCodeAsc(StockAssetType.STOCK))
                .thenReturn(List.of(stockItem));

        List<StockItemResponse> result = investmentService.getStockItems("stock");

        assertEquals(1, result.size());
        assertEquals("005930", result.get(0).stockCode());
        assertEquals(StockAssetType.STOCK, result.get(0).assetType());
    }

    @Test
    @DisplayName("지원하지 않는 자산유형 필터면 예외가 발생한다.")
    void getStockItemsFailWhenInvalidAssetType() {
        InvestmentException exception = assertThrows(
                InvestmentException.class,
                () -> investmentService.getStockItems("CRYPTO")
        );

        assertEquals(InvestmentErrorCode.INVALID_ASSET_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("종목코드 기준 현재가를 조회할 수 있다.")
    void getCurrentStockPriceSuccess() {
        when(stockPriceRepository.findCurrentPrice("005930")).thenReturn(Optional.of(73500L));

        StockPriceResponse response = investmentService.getCurrentStockPrice("005930");

        assertEquals("005930", response.stockCode());
        assertEquals(73500L, response.currentPrice());
    }

    @Test
    @DisplayName("현재가가 없으면 예외가 발생한다.")
    void getCurrentStockPriceFailWhenNotFound() {
        when(stockPriceRepository.findCurrentPrice("005930")).thenReturn(Optional.empty());

        InvestmentException exception = assertThrows(
                InvestmentException.class,
                () -> investmentService.getCurrentStockPrice("005930")
        );

        assertEquals(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND, exception.getErrorCode());
    }
}
