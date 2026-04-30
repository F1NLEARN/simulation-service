package com.finlearn.simulationservice.domain.holding.entity;

import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.exception.HoldingDomainException;
import com.finlearn.simulationservice.domain.holding.exception.HoldingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final String HOLDING_NAME = "삼성전자";
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final int SEASON_NUMBER = 1;
    private static final String INSTRUMENT_CODE = "005930";
    private static final long QUANTITY = 10L;
    private static final long AVERAGE_BUY_PRICE = 75_000L;
    private static final long CURRENT_PRICE = 80_000L;
    // totalBuyAmount = 10 * 75000 = 750000
    // valuationAmount = 10 * 80000 = 800000
    // unrealizedProfitLoss = 800000 - 750000 = 50000
    // returnRate = 50000 / 750000 * 100 = 6.67%

    private CreateHoldingCommand defaultCommand() {
        return new CreateHoldingCommand(
                ACCOUNT_ID, HOLDING_NAME, SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, QUANTITY, AVERAGE_BUY_PRICE, CURRENT_PRICE
        );
    }

    private Holding createDefault() {
        return Holding.create(defaultCommand());
    }

    @Test
    @DisplayName("CreateHoldingCommand로 Holding을 생성하면 파생 값이 자동 계산된다.")
    void create_withCommand_derivedValuesCalculated() {
        Holding holding = createDefault();

        assertThat(holding.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(holding.getHoldingName()).isEqualTo(HOLDING_NAME);
        assertThat(holding.getSeasonId()).isEqualTo(SEASON_ID);
        assertThat(holding.getSeasonNumber()).isEqualTo(SEASON_NUMBER);
        assertThat(holding.getInstrumentCode()).isEqualTo(INSTRUMENT_CODE);
        assertThat(holding.getQuantity()).isEqualTo(QUANTITY);
        assertThat(holding.getAverageBuyPrice()).isEqualTo(AVERAGE_BUY_PRICE);
        assertThat(holding.getCurrentPrice()).isEqualTo(CURRENT_PRICE);
        assertThat(holding.getTotalBuyAmount()).isEqualTo(750_000L);
        assertThat(holding.getValuationAmount()).isEqualTo(800_000L);
        assertThat(holding.getUnrealizedProfitLoss()).isEqualTo(50_000L);
        assertThat(holding.getReturnRate()).isEqualByComparingTo(new BigDecimal("6.67"));
    }

    @Test
    @DisplayName("quantity가 음수이면 INVALID_QUANTITY 코드로 예외가 발생한다.")
    void create_withNegativeQuantity_throwsException() {
        CreateHoldingCommand command = new CreateHoldingCommand(
                ACCOUNT_ID, HOLDING_NAME, SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, -1L, AVERAGE_BUY_PRICE, CURRENT_PRICE
        );

        assertThatThrownBy(() -> Holding.create(command))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.INVALID_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("averageBuyPrice가 음수이면 INVALID_AVERAGE_BUY_PRICE 코드로 예외가 발생한다.")
    void create_withNegativeAverageBuyPrice_throwsException() {
        CreateHoldingCommand command = new CreateHoldingCommand(
                ACCOUNT_ID, HOLDING_NAME, SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, QUANTITY, -1L, CURRENT_PRICE
        );

        assertThatThrownBy(() -> Holding.create(command))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.INVALID_AVERAGE_BUY_PRICE.getMessage());
    }

    @Test
    @DisplayName("currentPrice가 0 이하이면 INVALID_CURRENT_PRICE 코드로 예외가 발생한다.")
    void create_withNonPositiveCurrentPrice_throwsException() {
        CreateHoldingCommand command = new CreateHoldingCommand(
                ACCOUNT_ID, HOLDING_NAME, SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, QUANTITY, AVERAGE_BUY_PRICE, 0L
        );

        assertThatThrownBy(() -> Holding.create(command))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.INVALID_CURRENT_PRICE.getMessage());
    }

    @Test
    @DisplayName("quantity가 0이면 isEmpty()가 true를 반환한다.")
    void isEmpty_whenQuantityIsZero_returnsTrue() {
        CreateHoldingCommand command = new CreateHoldingCommand(
                ACCOUNT_ID, HOLDING_NAME, SEASON_ID, SEASON_NUMBER,
                INSTRUMENT_CODE, 0L, 0L, CURRENT_PRICE
        );

        assertThat(Holding.create(command).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("quantity가 1 이상이면 isEmpty()가 false를 반환한다.")
    void isEmpty_whenQuantityIsPositive_returnsFalse() {
        assertThat(createDefault().isEmpty()).isFalse();
    }

    @Test
    @DisplayName("추가 매수 시 수량, 총 매입금액, 평균 매입가, 파생 값이 갱신된다.")
    void addBuy_updatesAllFields() {
        Holding holding = createDefault();
        // 10주 @ 75000 = 750000
        // 추가 5주 @ 90000 = 450000
        // 합계: 15주, 1200000원, 평균 80000
        holding.addBuy(5L, 90_000L);

        assertThat(holding.getQuantity()).isEqualTo(15L);
        assertThat(holding.getTotalBuyAmount()).isEqualTo(1_200_000L);
        assertThat(holding.getAverageBuyPrice()).isEqualTo(80_000L);
        assertThat(holding.getValuationAmount()).isEqualTo(15L * CURRENT_PRICE);
        assertThat(holding.getUnrealizedProfitLoss()).isEqualTo(15L * CURRENT_PRICE - 1_200_000L);
    }

    @Test
    @DisplayName("추가 매수 수량이 0 이하이면 INVALID_BUY_QUANTITY 코드로 예외가 발생한다.")
    void addBuy_withNonPositiveQuantity_throwsException() {
        assertThatThrownBy(() -> createDefault().addBuy(0L, 80_000L))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.INVALID_BUY_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("추가 매수 가격이 0 이하이면 INVALID_BUY_PRICE 코드로 예외가 발생한다.")
    void addBuy_withNonPositivePrice_throwsException() {
        assertThatThrownBy(() -> createDefault().addBuy(5L, 0L))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.INVALID_BUY_PRICE.getMessage());
    }

    @Test
    @DisplayName("매도 시 수량, 총 매입금액, 파생 값이 갱신된다.")
    void sell_updatesAllFields() {
        Holding holding = createDefault();
        // 10주 @ 75000, 3주 매도 → 7주, totalBuyAmount = 75000 * 7 = 525000
        holding.sell(3L);

        assertThat(holding.getQuantity()).isEqualTo(7L);
        assertThat(holding.getTotalBuyAmount()).isEqualTo(525_000L);
        assertThat(holding.getValuationAmount()).isEqualTo(7L * CURRENT_PRICE);
        assertThat(holding.getUnrealizedProfitLoss()).isEqualTo(7L * CURRENT_PRICE - 525_000L);
    }

    @Test
    @DisplayName("전량 매도 시 수량이 0이 되고 isEmpty()가 true를 반환한다.")
    void sell_allQuantity_becomesEmpty() {
        Holding holding = createDefault();

        holding.sell(QUANTITY);

        assertThat(holding.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("매도 수량이 보유 수량을 초과하면 EXCEED_SELL_QUANTITY 코드로 예외가 발생한다.")
    void sell_withExceedingQuantity_throwsException() {
        assertThatThrownBy(() -> createDefault().sell(QUANTITY + 1))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.EXCEED_SELL_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("매도 수량이 0 이하이면 INVALID_SELL_QUANTITY 코드로 예외가 발생한다.")
    void sell_withNonPositiveQuantity_throwsException() {
        assertThatThrownBy(() -> createDefault().sell(0L))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.INVALID_SELL_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("현재가 갱신 시 평가금액, 미실현 손익, 수익률이 재계산된다.")
    void updateCurrentPrice_recalculatesDerivedValues() {
        Holding holding = createDefault();
        // 현재가 80000 → 70000으로 하락
        // valuationAmount = 10 * 70000 = 700000
        // unrealizedProfitLoss = 700000 - 750000 = -50000
        // returnRate = -50000 / 750000 * 100 = -6.67%
        holding.updateCurrentPrice(70_000L);

        assertThat(holding.getCurrentPrice()).isEqualTo(70_000L);
        assertThat(holding.getValuationAmount()).isEqualTo(700_000L);
        assertThat(holding.getUnrealizedProfitLoss()).isEqualTo(-50_000L);
        assertThat(holding.getReturnRate()).isEqualByComparingTo(new BigDecimal("-6.67"));
    }

    @Test
    @DisplayName("현재가가 0 이하이면 INVALID_UPDATE_PRICE 코드로 예외가 발생한다.")
    void updateCurrentPrice_withNonPositivePrice_throwsException() {
        assertThatThrownBy(() -> createDefault().updateCurrentPrice(0L))
                .isInstanceOf(HoldingDomainException.class)
                .hasMessage(HoldingErrorCode.INVALID_UPDATE_PRICE.getMessage());
    }
}