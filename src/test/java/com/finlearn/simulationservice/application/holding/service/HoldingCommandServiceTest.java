package com.finlearn.simulationservice.application.holding.service;

import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.exception.HoldingDomainException;
import com.finlearn.simulationservice.domain.holding.exception.HoldingNotFoundException;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.tradehistory.event.StockBoughtEvent;
import com.finlearn.simulationservice.domain.tradehistory.event.StockSoldEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingCommandServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @InjectMocks
    private HoldingCommandService holdingCommandService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final int SEASON_NUMBER = 1;
    private static final String INSTRUMENT_CODE = "005930";

    private StockBoughtEvent buyEvent(long quantity, long price) {
        return new StockBoughtEvent(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, quantity, price, quantity * price, 0L, LocalDateTime.now()
        );
    }

    private StockSoldEvent sellEvent(long quantity, long price) {
        return new StockSoldEvent(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, quantity, price, quantity * price, 0L, LocalDateTime.now()
        );
    }

    private Holding existingHolding(long quantity, long avgPrice) {
        return Holding.create(new CreateHoldingCommand(
                ACCOUNT_ID, "삼성전자", SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, quantity, avgPrice, avgPrice
        ));
    }

    @Test
    @DisplayName("최초 매수 시 신규 Holding을 생성한다.")
    void applyBuyResult_firstBuy_createsNewHolding() {
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, INSTRUMENT_CODE))
                .thenReturn(Optional.empty());
        when(stockItemRepository.findByStockCode(INSTRUMENT_CODE))
                .thenReturn(Optional.of(StockItem.create("삼성전자", INSTRUMENT_CODE, StockAssetType.STOCK)));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        holdingCommandService.applyBuyResult(buyEvent(10L, 75_000L));

        verify(holdingRepository).save(any(Holding.class));
    }

    @Test
    @DisplayName("추가 매수 시 수량이 증가하고 평균 단가가 재계산된다.")
    void applyBuyResult_additionalBuy_updatesQuantityAndAveragePrice() {
        // 기존: 10주 @ 70,000 → totalBuyAmount = 700,000
        // 추가: 10주 @ 80,000 → totalBuyAmount = 1,500,000 → avg = 75,000
        Holding holding = existingHolding(10L, 70_000L);
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, INSTRUMENT_CODE))
                .thenReturn(Optional.of(holding));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        holdingCommandService.applyBuyResult(buyEvent(10L, 80_000L));

        assertThat(holding.getQuantity()).isEqualTo(20L);
        assertThat(holding.getAverageBuyPrice()).isEqualTo(75_000L);
    }

    @Test
    @DisplayName("부분 매도 시 보유 수량이 감소한다.")
    void applySellResult_partialSell_decreasesQuantity() {
        Holding holding = existingHolding(10L, 75_000L);
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, INSTRUMENT_CODE))
                .thenReturn(Optional.of(holding));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        holdingCommandService.applySellResult(sellEvent(3L, 80_000L));

        assertThat(holding.getQuantity()).isEqualTo(7L);
    }

    @Test
    @DisplayName("전량 매도 시 보유 수량이 0이 된다.")
    void applySellResult_fullSell_quantityBecomesZero() {
        Holding holding = existingHolding(10L, 75_000L);
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, INSTRUMENT_CODE))
                .thenReturn(Optional.of(holding));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        holdingCommandService.applySellResult(sellEvent(10L, 80_000L));

        assertThat(holding.getQuantity()).isEqualTo(0L);
        assertThat(holding.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("보유 수량을 초과한 매도 시 HoldingDomainException을 던진다.")
    void applySellResult_exceedQuantity_throwsHoldingDomainException() {
        Holding holding = existingHolding(5L, 75_000L);
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, INSTRUMENT_CODE))
                .thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> holdingCommandService.applySellResult(sellEvent(10L, 80_000L)))
                .isInstanceOf(HoldingDomainException.class);
    }

    @Test
    @DisplayName("매도할 Holding이 없으면 HoldingNotFoundException을 던진다.")
    void applySellResult_holdingNotFound_throwsHoldingNotFoundException() {
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, INSTRUMENT_CODE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> holdingCommandService.applySellResult(sellEvent(5L, 80_000L)))
                .isInstanceOf(HoldingNotFoundException.class);
    }

    @Test
    @DisplayName("매도 처리 중 예외가 발생하면 Holding이 저장되지 않는다.")
    void applySellResult_onException_holdingIsNotSaved() {
        Holding holding = existingHolding(5L, 75_000L);
        when(holdingRepository.findByAccountIdAndInstrumentCode(ACCOUNT_ID, INSTRUMENT_CODE))
                .thenReturn(Optional.of(holding));

        // 보유 수량(5)보다 많은 수량(10)으로 매도 시도 → Holding.sell() 내부에서 예외 발생
        assertThatThrownBy(() -> holdingCommandService.applySellResult(sellEvent(10L, 80_000L)))
                .isInstanceOf(HoldingDomainException.class);

        verify(holdingRepository, never()).save(any());
    }
}