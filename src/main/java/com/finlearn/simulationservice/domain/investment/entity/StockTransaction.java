package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.StockTransactionType;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trade_history_id")
    private UUID tradeHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount investmentAccount;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "season_number", nullable = false)
    private int seasonNumber;

    @Column(name = "instrument_code", nullable = false, length = 20)
    private String instrumentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 10)
    private StockTransactionType tradeType;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "trade_price", nullable = false)
    private long tradePrice;

    @Column(name = "total_trade_amount", nullable = false)
    private long totalTradeAmount;

    @Column(name = "trade_at", nullable = false)
    private LocalDateTime tradeAt;

    @Column(name = "cash_balance_after_trade", nullable = false)
    private long cashBalanceAfterTrade;

    @Builder
    private StockTransaction(InvestmentAccount investmentAccount, UUID seasonId, int seasonNumber,
                             String instrumentCode, StockTransactionType tradeType, long quantity,
                             long tradePrice, long cashBalanceAfterTrade, LocalDateTime tradeAt) {
        this.investmentAccount = investmentAccount;
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.instrumentCode = instrumentCode;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.tradePrice = tradePrice;
        this.totalTradeAmount = tradePrice * quantity;
        this.cashBalanceAfterTrade = cashBalanceAfterTrade;
        this.tradeAt = tradeAt != null ? tradeAt : LocalDateTime.now();
    }

    public static StockTransaction buy(InvestmentAccount account, UUID seasonId, int seasonNumber,
                                       String instrumentCode, long quantity, long tradePrice,
                                       long cashBalanceAfterTrade, LocalDateTime tradeAt) {
        return create(account, seasonId, seasonNumber, instrumentCode, StockTransactionType.BUY,
                quantity, tradePrice, cashBalanceAfterTrade, tradeAt);
    }

    public static StockTransaction sell(InvestmentAccount account, UUID seasonId, int seasonNumber,
                                        String instrumentCode, long quantity, long tradePrice,
                                        long cashBalanceAfterTrade, LocalDateTime tradeAt) {
        return create(account, seasonId, seasonNumber, instrumentCode, StockTransactionType.SELL,
                quantity, tradePrice, cashBalanceAfterTrade, tradeAt);
    }

    private static StockTransaction create(InvestmentAccount account, UUID seasonId, int seasonNumber,
                                           String instrumentCode, StockTransactionType tradeType,
                                           long quantity, long tradePrice,
                                           long cashBalanceAfterTrade, LocalDateTime tradeAt) {
        if (quantity <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }
        if (tradePrice <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_STOCK_PRICE);
        }
        return StockTransaction.builder()
                .investmentAccount(account)
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .instrumentCode(instrumentCode)
                .tradeType(tradeType)
                .quantity(quantity)
                .tradePrice(tradePrice)
                .cashBalanceAfterTrade(cashBalanceAfterTrade)
                .tradeAt(tradeAt)
                .build();
    }
}
