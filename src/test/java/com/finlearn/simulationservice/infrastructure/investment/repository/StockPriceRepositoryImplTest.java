package com.finlearn.simulationservice.infrastructure.investment.repository;

import com.finlearn.simulationservice.domain.investment.dto.ResolvedStockPrice;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.infrastructure.investment.client.KisStockPriceClient;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockPriceRepositoryImplTest {

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private KisStockPriceClient kisStockPriceClient;

    @InjectMocks
    private StockPriceRepositoryImpl stockPriceRepository;

    @Test
    @DisplayName("종목코드로 현재가를 조회할 수 있다.")
    void findCurrentPriceSuccess() {
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK, 73500L);
        when(kisStockPriceClient.findCurrentPrice("005930")).thenReturn(Optional.empty());
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.of(stockItem));

        Optional<ResolvedStockPrice> result = stockPriceRepository.findCurrentPriceWithSource("005930");

        assertTrue(result.isPresent());
        assertEquals(73500L, result.get().currentPrice());
        assertEquals(StockPriceSource.DB_FALLBACK, result.get().source());
        verify(kisStockPriceClient).findCurrentPrice("005930");
        verify(stockItemRepository).findByStockCode("005930");
    }

    @Test
    @DisplayName("KIS 현재가를 조회할 수 있으면 종목 마스터의 캐시 가격을 갱신한다.")
    void findCurrentPricePreferKisPrice() {
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK, 73500L);
        when(kisStockPriceClient.findCurrentPrice("005930")).thenReturn(Optional.of(74000L));
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.of(stockItem));

        Optional<ResolvedStockPrice> result = stockPriceRepository.findCurrentPriceWithSource("005930");

        assertTrue(result.isPresent());
        assertEquals(74000L, result.get().currentPrice());
        assertEquals(StockPriceSource.KIS, result.get().source());
        assertEquals(74000L, stockItem.getCurrentPrice());
        assertTrue(stockItem.getCurrentPriceUpdatedAt() != null);
        verify(stockItemRepository).findByStockCode("005930");
    }

    @Test
    @DisplayName("현재가가 null이면 empty를 반환한다.")
    void findCurrentPriceEmptyWhenPriceIsNull() {
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK, null);
        when(kisStockPriceClient.findCurrentPrice("005930")).thenReturn(Optional.empty());
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.of(stockItem));

        Optional<ResolvedStockPrice> result = stockPriceRepository.findCurrentPriceWithSource("005930");

        assertTrue(result.isEmpty());
    }
}
