package com.finlearn.simulationservice.domain.holding.entity;

import com.finlearn.common.exception.BadRequestException;
import com.finlearn.simulationservice.domain.holding.enums.InstrumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID STOCK_ID = UUID.randomUUID();
    private static final String STOCK_CODE = "005930";
    private static final String STOCK_NAME = "삼성전자";
    private static final InstrumentType INSTRUMENT_TYPE = InstrumentType.STOCK;
    private static final long QUANTITY = 10L;
    private static final BigDecimal AVERAGE_BUY_PRICE = new BigDecimal("75000");
    private static final BigDecimal TOTAL_BUY_AMOUNT = new BigDecimal("750000");

    @Test
    @DisplayName("정상적인 값으로 Holding을 생성할 수 있다.")
    void create_withValidValues_success() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );

        assertThat(holding.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(holding.getStockId()).isEqualTo(STOCK_ID);
        assertThat(holding.getStockCode()).isEqualTo(STOCK_CODE);
        assertThat(holding.getStockName()).isEqualTo(STOCK_NAME);
        assertThat(holding.getInstrumentType()).isEqualTo(INSTRUMENT_TYPE);
        assertThat(holding.getQuantity()).isEqualTo(QUANTITY);
        assertThat(holding.getAverageBuyPrice()).isEqualByComparingTo(AVERAGE_BUY_PRICE);
        assertThat(holding.getTotalBuyAmount()).isEqualByComparingTo(TOTAL_BUY_AMOUNT);
    }

    @Test
    @DisplayName("quantity가 0이어도 Holding을 생성할 수 있다.")
    void create_withZeroQuantity_success() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, 0L, BigDecimal.ZERO, BigDecimal.ZERO
        );

        assertThat(holding.getQuantity()).isZero();
    }

    @Test
    @DisplayName("quantity가 음수이면 생성에 실패한다.")
    void create_withNegativeQuantity_throwsException() {
        assertThatThrownBy(() -> Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, -1L, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("averageBuyPrice가 음수이면 생성에 실패한다.")
    void create_withNegativeAverageBuyPrice_throwsException() {
        assertThatThrownBy(() -> Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, new BigDecimal("-1"), TOTAL_BUY_AMOUNT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("totalBuyAmount가 음수이면 생성에 실패한다.")
    void create_withNegativeTotalBuyAmount_throwsException() {
        assertThatThrownBy(() -> Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, new BigDecimal("-1")
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("quantity가 0이면 isEmpty()가 true를 반환한다.")
    void isEmpty_whenQuantityIsZero_returnsTrue() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, 0L, BigDecimal.ZERO, BigDecimal.ZERO
        );

        assertThat(holding.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("quantity가 1 이상이면 isEmpty()가 false를 반환한다.")
    void isEmpty_whenQuantityIsPositive_returnsFalse() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );

        assertThat(holding.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("추가 매수 시 수량, 총 매입금액, 평균 매입가가 갱신된다.")
    void addBuy_updatesQuantityAndAverageBuyPrice() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );
        // 10주 @ 75000 = 750000
        // 추가 5주 @ 80000 = 400000
        // 합계: 15주, 1150000원, 평균 76666.6667
        holding.addBuy(5L, new BigDecimal("80000"));

        assertThat(holding.getQuantity()).isEqualTo(15L);
        assertThat(holding.getTotalBuyAmount()).isEqualByComparingTo(new BigDecimal("1150000"));
        assertThat(holding.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("76666.6667"));
    }

    @Test
    @DisplayName("추가 매수 수량이 0 이하이면 예외가 발생한다.")
    void addBuy_withNonPositiveQuantity_throwsException() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );

        assertThatThrownBy(() -> holding.addBuy(0L, new BigDecimal("80000")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("추가 매수 가격이 0 이하이면 예외가 발생한다.")
    void addBuy_withNonPositivePrice_throwsException() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );

        assertThatThrownBy(() -> holding.addBuy(5L, BigDecimal.ZERO))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("매도 시 수량과 총 매입금액이 감소한다.")
    void sell_decreasesQuantityAndTotalBuyAmount() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );
        // 10주 @ 75000, 3주 매도 -> 7주 남음, 총 매입금액 = 75000 * 7 = 525000
        holding.sell(3L);

        assertThat(holding.getQuantity()).isEqualTo(7L);
        assertThat(holding.getTotalBuyAmount()).isEqualByComparingTo(new BigDecimal("525000.0000"));
    }

    @Test
    @DisplayName("전량 매도 시 수량이 0이 되고 isEmpty()가 true를 반환한다.")
    void sell_allQuantity_becomesEmpty() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );

        holding.sell(QUANTITY);

        assertThat(holding.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("매도 수량이 보유 수량을 초과하면 예외가 발생한다.")
    void sell_withExceedingQuantity_throwsException() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );

        assertThatThrownBy(() -> holding.sell(QUANTITY + 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("매도 수량이 0 이하이면 예외가 발생한다.")
    void sell_withNonPositiveQuantity_throwsException() {
        Holding holding = Holding.create(
                ACCOUNT_ID, STOCK_ID, STOCK_CODE, STOCK_NAME,
                INSTRUMENT_TYPE, QUANTITY, AVERAGE_BUY_PRICE, TOTAL_BUY_AMOUNT
        );

        assertThatThrownBy(() -> holding.sell(0L))
                .isInstanceOf(BadRequestException.class);
    }
}