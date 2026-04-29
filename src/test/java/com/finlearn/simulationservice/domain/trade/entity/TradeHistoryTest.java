package com.finlearn.simulationservice.domain.trade.entity;

import com.finlearn.common.exception.BadRequestException;
import com.finlearn.simulationservice.domain.trade.enums.TradeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeHistoryTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final int SEASON_NUMBER = 1;
    private static final String INSTRUMENT_CODE = "005930";
    private static final long QUANTITY = 5L;
    private static final long TRADE_PRICE = 75_000L;
    private static final LocalDateTime TRADE_AT = LocalDateTime.of(2026, 4, 29, 10, 0);
    private static final long CASH_BALANCE_AFTER_TRADE = 625_000L;

    @Test
    @DisplayName("유효한 값으로 매수 거래 이력을 생성할 수 있다.")
    void buy_withValidValues_success() {
        TradeHistory tradeHistory = TradeHistory.buy(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER, INSTRUMENT_CODE,
                QUANTITY, TRADE_PRICE, TRADE_AT, CASH_BALANCE_AFTER_TRADE
        );

        assertThat(tradeHistory.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(tradeHistory.getSeasonId()).isEqualTo(SEASON_ID);
        assertThat(tradeHistory.getSeasonNumber()).isEqualTo(SEASON_NUMBER);
        assertThat(tradeHistory.getInstrumentCode()).isEqualTo(INSTRUMENT_CODE);
        assertThat(tradeHistory.getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(tradeHistory.getQuantity()).isEqualTo(QUANTITY);
        assertThat(tradeHistory.getTradePrice()).isEqualTo(TRADE_PRICE);
        assertThat(tradeHistory.getTradeAt()).isEqualTo(TRADE_AT);
        assertThat(tradeHistory.getCashBalanceAfterTrade()).isEqualTo(CASH_BALANCE_AFTER_TRADE);
    }

    @Test
    @DisplayName("유효한 값으로 매도 거래 이력을 생성할 수 있다.")
    void sell_withValidValues_success() {
        TradeHistory tradeHistory = TradeHistory.sell(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER, INSTRUMENT_CODE,
                QUANTITY, TRADE_PRICE, TRADE_AT, CASH_BALANCE_AFTER_TRADE
        );

        assertThat(tradeHistory.getTradeType()).isEqualTo(TradeType.SELL);
    }

    @Test
    @DisplayName("총 거래 금액은 수량 × 단가로 자동 계산된다.")
    void buy_totalTradeAmount_isCalculatedAutomatically() {
        TradeHistory tradeHistory = TradeHistory.buy(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER, INSTRUMENT_CODE,
                QUANTITY, TRADE_PRICE, TRADE_AT, CASH_BALANCE_AFTER_TRADE
        );

        assertThat(tradeHistory.getTotalTradeAmount()).isEqualTo(QUANTITY * TRADE_PRICE); // 375_000
    }

    @Test
    @DisplayName("거래 수량이 0 이하이면 생성에 실패한다.")
    void create_withNonPositiveQuantity_throwsException() {
        assertThatThrownBy(() -> TradeHistory.buy(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER, INSTRUMENT_CODE,
                0L, TRADE_PRICE, TRADE_AT, CASH_BALANCE_AFTER_TRADE
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("거래 가격이 0 이하이면 생성에 실패한다.")
    void create_withNonPositivePrice_throwsException() {
        assertThatThrownBy(() -> TradeHistory.buy(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER, INSTRUMENT_CODE,
                QUANTITY, 0L, TRADE_AT, CASH_BALANCE_AFTER_TRADE
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("tradeType이 null이면 생성에 실패한다.")
    void create_withNullTradeType_throwsException() {
        assertThatThrownBy(() -> TradeHistory.builder()
                .accountId(ACCOUNT_ID)
                .seasonId(SEASON_ID)
                .seasonNumber(SEASON_NUMBER)
                .instrumentCode(INSTRUMENT_CODE)
                .tradeType(null)
                .quantity(QUANTITY)
                .tradePrice(TRADE_PRICE)
                .tradeAt(TRADE_AT)
                .cashBalanceAfterTrade(CASH_BALANCE_AFTER_TRADE)
                .build()
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("생성 후 필드를 변경하는 메서드가 없어 불변으로 관리된다.")
    void tradeHistory_isImmutableAfterCreation() {
        TradeHistory tradeHistory = TradeHistory.buy(
                ACCOUNT_ID, SEASON_ID, SEASON_NUMBER, INSTRUMENT_CODE,
                QUANTITY, TRADE_PRICE, TRADE_AT, CASH_BALANCE_AFTER_TRADE
        );

        assertThat(tradeHistory.getQuantity()).isEqualTo(QUANTITY);
        assertThat(tradeHistory.getTradePrice()).isEqualTo(TRADE_PRICE);
    }
}