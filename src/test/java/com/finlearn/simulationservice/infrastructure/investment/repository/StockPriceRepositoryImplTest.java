package com.finlearn.simulationservice.infrastructure.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import java.math.BigDecimal;
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

    @InjectMocks
    private StockPriceRepositoryImpl stockPriceRepository;

    @Test
    @DisplayName("종목코드로 현재가를 조회할 수 있다.")
    void findCurrentPriceSuccess() {
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK, new BigDecimal("73500.00"));
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.of(stockItem));

        Optional<BigDecimal> result = stockPriceRepository.findCurrentPrice(StockAssetType.STOCK, "005930");

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("73500.00"), result.get());
        verify(stockItemRepository).findByStockCode("005930");
    }

    @Test
    @DisplayName("현재가가 null이면 empty를 반환한다.")
    void findCurrentPriceEmptyWhenPriceIsNull() {
        StockItem stockItem = StockItem.create("삼성전자", "005930", StockAssetType.STOCK, null);
        when(stockItemRepository.findByStockCode("005930")).thenReturn(Optional.of(stockItem));

        Optional<BigDecimal> result = stockPriceRepository.findCurrentPrice(StockAssetType.STOCK, "005930");

        assertTrue(result.isEmpty());
    }
}
