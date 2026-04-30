package com.finlearn.simulationservice.domain.holding.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.common.exception.BadRequestException;
import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Column(nullable = false, length = 10)
    private String holdingName;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private int seasonNumber;

    @Column(nullable = false, length = 20)
    private String instrumentCode;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long averageBuyPrice;

    @Column(nullable = false)
    private long currentPrice;

    @Column(nullable = false)
    private long totalBuyAmount;

    @Column(nullable = false)
    private long valuationAmount;

    @Column(nullable = false)
    private long unrealizedProfitLoss;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal returnRate;

    @Builder
    private Holding(UUID accountId, String holdingName, UUID seasonId, int seasonNumber,
                    String instrumentCode, long quantity, long averageBuyPrice, long currentPrice) {
        validate(accountId, holdingName, seasonId, seasonNumber, instrumentCode, quantity, averageBuyPrice, currentPrice);
        this.accountId = accountId;
        this.holdingName = holdingName;
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.instrumentCode = instrumentCode;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.currentPrice = currentPrice;
        this.totalBuyAmount = quantity * averageBuyPrice;
        recalculateDerived();
    }

    public static Holding create(CreateHoldingCommand command) {
        return Holding.builder()
                .accountId(command.accountId())
                .holdingName(command.holdingName())
                .seasonId(command.seasonId())
                .seasonNumber(command.seasonNumber())
                .instrumentCode(command.instrumentCode())
                .quantity(command.quantity())
                .averageBuyPrice(command.averageBuyPrice())
                .currentPrice(command.currentPrice())
                .build();
    }

    public void addBuy(long buyQuantity, long buyPrice) {
        if (buyQuantity <= 0) {
            throw new BadRequestException("buyQuantity", "추가 매수 수량은 0보다 커야 합니다.");
        }
        if (buyPrice <= 0) {
            throw new BadRequestException("buyPrice", "매수 가격은 0보다 커야 합니다.");
        }
        this.totalBuyAmount += buyQuantity * buyPrice;
        this.quantity += buyQuantity;
        this.averageBuyPrice = BigDecimal.valueOf(this.totalBuyAmount)
                .divide(BigDecimal.valueOf(this.quantity), 0, RoundingMode.HALF_UP)
                .longValue();
        recalculateDerived();
    }

    public void sell(long sellQuantity) {
        if (sellQuantity <= 0) {
            throw new BadRequestException("sellQuantity", "매도 수량은 0보다 커야 합니다.");
        }
        if (sellQuantity > this.quantity) {
            throw new BadRequestException("sellQuantity", "매도 수량이 보유 수량을 초과할 수 없습니다.");
        }
        this.quantity -= sellQuantity;
        this.totalBuyAmount = this.averageBuyPrice * this.quantity;
        recalculateDerived();
    }

    public void updateCurrentPrice(long currentPrice) {
        if (currentPrice <= 0) {
            throw new BadRequestException("currentPrice", "현재가는 0보다 커야 합니다.");
        }
        this.currentPrice = currentPrice;
        recalculateDerived();
    }

    public boolean isEmpty() {
        return this.quantity == 0;
    }

    private void recalculateDerived() {
        this.valuationAmount = this.quantity * this.currentPrice;
        this.unrealizedProfitLoss = this.valuationAmount - this.totalBuyAmount;
        this.returnRate = this.totalBuyAmount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(this.unrealizedProfitLoss)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(this.totalBuyAmount), 2, RoundingMode.HALF_UP);
    }

    private static void validate(UUID accountId, String holdingName, UUID seasonId, int seasonNumber,
                                  String instrumentCode, long quantity, long averageBuyPrice, long currentPrice) {
        if (accountId == null) {
            throw new BadRequestException("accountId", "accountId는 null일 수 없습니다.");
        }
        if (holdingName == null || holdingName.isBlank()) {
            throw new BadRequestException("holdingName", "holdingName은 blank일 수 없습니다.");
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
        if (quantity < 0) {
            throw new BadRequestException("quantity", "quantity는 0 이상이어야 합니다.");
        }
        if (averageBuyPrice < 0) {
            throw new BadRequestException("averageBuyPrice", "averageBuyPrice는 0 이상이어야 합니다.");
        }
        if (currentPrice <= 0) {
            throw new BadRequestException("currentPrice", "currentPrice는 0보다 커야 합니다.");
        }
    }
}
