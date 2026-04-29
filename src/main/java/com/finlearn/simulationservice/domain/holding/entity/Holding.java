package com.finlearn.simulationservice.domain.holding.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.common.exception.BadRequestException;
import com.finlearn.simulationservice.domain.holding.enums.InstrumentType;
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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "holding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID holdingId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private UUID stockId;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstrumentType instrumentType;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal averageBuyPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBuyAmount;

    @Builder
    private Holding(UUID accountId, UUID stockId, String stockCode, String stockName,
                    InstrumentType instrumentType, long quantity,
                    BigDecimal averageBuyPrice, BigDecimal totalBuyAmount) {
        validate(accountId, stockId, stockCode, stockName, instrumentType, quantity, averageBuyPrice, totalBuyAmount);
        this.accountId = accountId;
        this.stockId = stockId;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.instrumentType = instrumentType;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.totalBuyAmount = totalBuyAmount;
    }

    public static Holding create(UUID accountId, UUID stockId, String stockCode, String stockName,
                                 InstrumentType instrumentType, long quantity,
                                 BigDecimal averageBuyPrice, BigDecimal totalBuyAmount) {
        return Holding.builder()
                .accountId(accountId)
                .stockId(stockId)
                .stockCode(stockCode)
                .stockName(stockName)
                .instrumentType(instrumentType)
                .quantity(quantity)
                .averageBuyPrice(averageBuyPrice)
                .totalBuyAmount(totalBuyAmount)
                .build();
    }

    public void addBuy(long buyQuantity, BigDecimal buyPrice) {
        if (buyQuantity <= 0) {
            throw new BadRequestException("buyQuantity", "추가 매수 수량은 0보다 커야 합니다.");
        }
        if (buyPrice == null || buyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("buyPrice", "매수 가격은 0보다 커야 합니다.");
        }
        BigDecimal buyCost = buyPrice.multiply(BigDecimal.valueOf(buyQuantity));
        this.totalBuyAmount = this.totalBuyAmount.add(buyCost);
        this.quantity += buyQuantity;
        this.averageBuyPrice = this.totalBuyAmount.divide(
                BigDecimal.valueOf(this.quantity), 4, java.math.RoundingMode.HALF_UP);
    }

    public void sell(long sellQuantity) {
        if (sellQuantity <= 0) {
            throw new BadRequestException("sellQuantity", "매도 수량은 0보다 커야 합니다.");
        }
        if (sellQuantity > this.quantity) {
            throw new BadRequestException("sellQuantity", "매도 수량이 보유 수량을 초과할 수 없습니다.");
        }
        this.totalBuyAmount = this.averageBuyPrice.multiply(
                BigDecimal.valueOf(this.quantity - sellQuantity)).setScale(4, java.math.RoundingMode.HALF_UP);
        this.quantity -= sellQuantity;
    }

    public boolean isEmpty() {
        return this.quantity == 0;
    }

    private static void validate(UUID accountId, UUID stockId, String stockCode, String stockName,
                                 InstrumentType instrumentType, long quantity,
                                 BigDecimal averageBuyPrice, BigDecimal totalBuyAmount) {
        if (accountId == null) {
            throw new BadRequestException("accountId", "accountId는 null일 수 없습니다.");
        }
        if (stockId == null) {
            throw new BadRequestException("stockId", "stockId는 null일 수 없습니다.");
        }
        if (stockCode == null || stockCode.isBlank()) {
            throw new BadRequestException("stockCode", "stockCode는 blank일 수 없습니다.");
        }
        if (stockName == null || stockName.isBlank()) {
            throw new BadRequestException("stockName", "stockName은 blank일 수 없습니다.");
        }
        if (instrumentType == null) {
            throw new BadRequestException("instrumentType", "instrumentType은 null일 수 없습니다.");
        }
        if (quantity < 0) {
            throw new BadRequestException("quantity", "quantity는 0 이상이어야 합니다.");
        }
        if (averageBuyPrice == null || averageBuyPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("averageBuyPrice", "averageBuyPrice는 0 이상이어야 합니다.");
        }
        if (totalBuyAmount == null || totalBuyAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("totalBuyAmount", "totalBuyAmount는 0 이상이어야 합니다.");
        }
    }
}