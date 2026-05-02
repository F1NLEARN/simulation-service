package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvestmentAccountTest {

    private static SeasonParticipant testParticipant() {
        return new SeasonParticipant(UUID.randomUUID(), "테스트유저", UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("ACTIVE 계좌는 매수 성공 시 예수금을 차감하고 거래 이력을 남긴다.")
    void buySuccess() {
        InvestmentAccount account = InvestmentAccount.open(testParticipant(), 100000L);

        account.buy("005930", "삼성전자", 10, 5000L, LocalDateTime.now());

        assertEquals(50000L, account.getCurrentCashBalance());
        assertEquals(1, account.getHoldingStocks().size());
        assertEquals(1, account.getStockTransactions().size());
    }

    @Test
    @DisplayName("예수금이 부족하면 매수할 수 없다.")
    void buyFailInsufficientCash() {
        InvestmentAccount account = InvestmentAccount.open(testParticipant(), 1000L);

        assertThrows(InvestmentException.class,
                () -> account.buy("005930", "삼성전자", 10, 5000L, LocalDateTime.now()));
    }

    @Test
    @DisplayName("보유 수량보다 많은 매도는 실패한다.")
    void sellFailInsufficientHolding() {
        InvestmentAccount account = InvestmentAccount.open(testParticipant(), 100000L);
        account.buy("005930", "삼성전자", 2, 10000L, LocalDateTime.now());

        assertThrows(InvestmentException.class,
                () -> account.sell("005930", 3, 11000L, LocalDateTime.now()));
    }

    @Test
    @DisplayName("CLOSED 계좌는 매수/매도를 허용하지 않는다.")
    void closedAccountCannotTrade() {
        InvestmentAccount account = InvestmentAccount.open(testParticipant(), 100000L);
        account.close();

        assertThrows(InvestmentException.class,
                () -> account.buy("005930", "삼성전자", 1, 10000L, LocalDateTime.now()));
    }

    @Test
    @DisplayName("매도 후 실현 손익이 올바르게 계산된다.")
    void sellRealizedProfitLoss() {
        InvestmentAccount account = InvestmentAccount.open(testParticipant(), 100000L);
        account.buy("005930", "삼성전자", 10, 5000L, LocalDateTime.now());
        account.sell("005930", 10, 6000L, LocalDateTime.now());

        assertEquals(10000L, account.getRealizedProfitLoss());
        assertEquals(110000L, account.getCurrentCashBalance());
    }

    @Test
    @DisplayName("총 자산 금액은 현금 잔액과 보유 종목 평가금액의 합이다.")
    void totalAssetAmountIsCorrect() {
        InvestmentAccount account = InvestmentAccount.open(testParticipant(), 100000L);
        account.buy("005930", "삼성전자", 10, 5000L, LocalDateTime.now());

        assertEquals(50000L, account.getCurrentCashBalance());
        assertEquals(50000L, account.getTotalValuationAmount());
        assertEquals(100000L, account.getTotalAssetAmount());
    }
}
