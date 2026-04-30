package com.finlearn.simulationservice.domain.trade.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.common.exception.BadRequestException;
import com.finlearn.simulationservice.domain.trade.enums.TradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
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

    @Column(nullable = false, length = 20)
    private String instrumentCode;

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

    @Builder
    private TradeHistory(UUID accountId, UUID seasonId, int seasonNumber, String instrumentCode,
                         TradeType tradeType, long quantity, long tradePrice,
                         LocalDateTime tradeAt, long cashBalanceAfterTrade) {
        validate(accountId, seasonId, seasonNumber, instrumentCode, tradeType, quantity, tradePrice, tradeAt, cashBalanceAfterTrade);
        this.accountId = accountId;
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.instrumentCode = instrumentCode;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.tradePrice = tradePrice;
        this.totalTradeAmount = quantity * tradePrice;
        this.tradeAt = tradeAt;
        this.cashBalanceAfterTrade = cashBalanceAfterTrade;
    }

    public static TradeHistory buy(UUID accountId, UUID seasonId, int seasonNumber, String instrumentCode,
                                   long quantity, long tradePrice,
                                   LocalDateTime tradeAt, long cashBalanceAfterTrade) {
        return TradeHistory.builder()
                .accountId(accountId)
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .instrumentCode(instrumentCode)
                .tradeType(TradeType.BUY)
                .quantity(quantity)
                .tradePrice(tradePrice)
                .tradeAt(tradeAt)
                .cashBalanceAfterTrade(cashBalanceAfterTrade)
                .build();
    }

    public static TradeHistory sell(UUID accountId, UUID seasonId, int seasonNumber, String instrumentCode,
                                    long quantity, long tradePrice,
                                    LocalDateTime tradeAt, long cashBalanceAfterTrade) {
        return TradeHistory.builder()
                .accountId(accountId)
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .instrumentCode(instrumentCode)
                .tradeType(TradeType.SELL)
                .quantity(quantity)
                .tradePrice(tradePrice)
                .tradeAt(tradeAt)
                .cashBalanceAfterTrade(cashBalanceAfterTrade)
                .build();
    }

    private static void validate(UUID accountId, UUID seasonId, int seasonNumber, String instrumentCode,
                                  TradeType tradeType, long quantity, long tradePrice,
                                  LocalDateTime tradeAt, long cashBalanceAfterTrade) {
        if (accountId == null) {
            throw new BadRequestException("accountId", "accountId는 null일 수 없습니다.");
        }
        if (seasonId == null) {
            throw new BadRequestException("seasonId", "seasonId는 null일 수 없습니다.");
        }
        if (seasonNumber <= 0) {
            throw new BadRequestException("seasonNumber", "seasonNumber는 0보다 커야 합니다.");
        }
        if (instrumentCode == null || instrumentCode.isBlank()) {
            throw new BadRequestException("instrumentCode", "instrumentCode는 blank일 수 없습니다.");
        }
        if (tradeType == null) {
            throw new BadRequestException("tradeType", "tradeType은 null일 수 없습니다.");
        }
        if (quantity <= 0) {
            throw new BadRequestException("quantity", "거래 수량은 0보다 커야 합니다.");
        }
        if (tradePrice <= 0) {
            throw new BadRequestException("tradePrice", "거래 가격은 0보다 커야 합니다.");
        }
        if (tradeAt == null) {
            throw new BadRequestException("tradeAt", "거래 일시는 null일 수 없습니다.");
        }
        if (cashBalanceAfterTrade < 0) {
            throw new BadRequestException("cashBalanceAfterTrade", "거래 후 현금 잔액은 0 이상이어야 합니다.");
        }
    }
}