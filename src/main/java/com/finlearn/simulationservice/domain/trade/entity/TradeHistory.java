package com.finlearn.simulationservice.domain.trade.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.trade.command.CreateTradeHistoryCommand;
import com.finlearn.simulationservice.domain.trade.exception.TradeHistoryDomainException;
import com.finlearn.simulationservice.domain.trade.exception.TradeHistoryErrorCode;
import com.finlearn.simulationservice.domain.vo.InstrumentCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tradeHistoryId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private int seasonNumber;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "instrument_code", nullable = false, length = 20))
    private InstrumentCode instrumentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType tradeType;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long tradePrice;

    @Column(nullable = false)
    private long totalTradeAmount;

    @Column(nullable = false)
    private LocalDateTime tradeAt;

    @Column(nullable = false)
    private long cashBalanceAfterTrade;

    private TradeHistory(CreateTradeHistoryCommand command) {
        validate(command);
        this.accountId = command.accountId();
        this.seasonId = command.seasonId();
        this.seasonNumber = command.seasonNumber();
        this.instrumentCode = InstrumentCode.of(command.instrumentCode());
        this.tradeType = command.tradeType();
        this.quantity = command.quantity();
        this.tradePrice = command.tradePrice();
        this.totalTradeAmount = command.quantity() * command.tradePrice();
        this.tradeAt = command.tradeAt();
        this.cashBalanceAfterTrade = command.cashBalanceAfterTrade();
    }

    public static TradeHistory create(CreateTradeHistoryCommand command) {
        return new TradeHistory(command);
    }

    public static TradeHistory buy(UUID accountId, UUID seasonId, int seasonNumber, String instrumentCode,
                                   long quantity, long tradePrice,
                                   LocalDateTime tradeAt, long cashBalanceAfterTrade) {
        return create(new CreateTradeHistoryCommand(
                accountId, seasonId, seasonNumber, instrumentCode, TradeType.BUY,
                quantity, tradePrice, tradeAt, cashBalanceAfterTrade
        ));
    }

    public static TradeHistory sell(UUID accountId, UUID seasonId, int seasonNumber, String instrumentCode,
                                    long quantity, long tradePrice,
                                    LocalDateTime tradeAt, long cashBalanceAfterTrade) {
        return create(new CreateTradeHistoryCommand(
                accountId, seasonId, seasonNumber, instrumentCode, TradeType.SELL,
                quantity, tradePrice, tradeAt, cashBalanceAfterTrade
        ));
    }

    private static void validate(CreateTradeHistoryCommand command) {
        if (command.accountId() == null) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_ACCOUNT_ID);
        }
        if (command.seasonId() == null) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_SEASON_ID);
        }
        if (command.seasonNumber() <= 0) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_SEASON_NUMBER);
        }
        if (command.instrumentCode() == null || command.instrumentCode().isBlank()) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_INSTRUMENT_CODE);
        }
        if (command.tradeType() == null) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_TRADE_TYPE);
        }
        if (command.quantity() <= 0) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_QUANTITY);
        }
        if (command.tradePrice() <= 0) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_TRADE_PRICE);
        }
        if (command.tradeAt() == null) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_TRADE_AT);
        }
        if (command.cashBalanceAfterTrade() < 0) {
            throw new TradeHistoryDomainException(TradeHistoryErrorCode.INVALID_CASH_BALANCE);
        }
    }
}