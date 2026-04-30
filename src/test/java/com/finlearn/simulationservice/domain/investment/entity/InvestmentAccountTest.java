package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvestmentAccountTest {

    @Test
    @DisplayName("ACTIVE 계좌는 매수 성공 시 예수금을 차감하고 거래 이력을 남긴다.")
    void buySuccess() {
        InvestmentAccount account = InvestmentAccount.open(UUID.randomUUID(), new BigDecimal("100000.00"));

        account.buy(StockAssetType.STOCK, "005930", 10, new BigDecimal("5000"), LocalDateTime.now());

        assertEquals(new BigDecimal("50000.00"), account.getCashBalance());
        assertEquals(1, account.getHoldingStocks().size());
        assertEquals(1, account.getStockTransactions().size());
    }

    @Test
    @DisplayName("예수금이 부족하면 매수할 수 없다.")
    void buyFailInsufficientCash() {
        InvestmentAccount account = InvestmentAccount.open(UUID.randomUUID(), new BigDecimal("1000.00"));

        assertThrows(InvestmentException.class,
                () -> account.buy(StockAssetType.STOCK, "005930", 10, new BigDecimal("5000"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("보유 수량보다 많은 매도는 실패한다.")
    void sellFailInsufficientHolding() {
        InvestmentAccount account = InvestmentAccount.open(UUID.randomUUID(), new BigDecimal("100000.00"));
        account.buy(StockAssetType.STOCK, "005930", 2, new BigDecimal("10000"), LocalDateTime.now());

        assertThrows(InvestmentException.class,
                () -> account.sell(StockAssetType.STOCK, "005930", 3, new BigDecimal("11000"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("CLOSED 계좌는 매수/매도를 허용하지 않는다.")
    void closedAccountCannotTrade() {
        InvestmentAccount account = InvestmentAccount.open(UUID.randomUUID(), new BigDecimal("100000.00"));
        account.close();

        assertThrows(InvestmentException.class,
                () -> account.buy(StockAssetType.STOCK, "005930", 1, new BigDecimal("10000"), LocalDateTime.now()));
    }
}
