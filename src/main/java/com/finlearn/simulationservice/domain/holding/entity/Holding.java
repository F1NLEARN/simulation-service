package com.finlearn.simulationservice.domain.holding.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.exception.HoldingDomainException;
import com.finlearn.simulationservice.domain.holding.exception.HoldingErrorCode;
import com.finlearn.simulationservice.domain.vo.InstrumentCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
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

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "instrument_code", nullable = false, length = 20))
    private InstrumentCode instrumentCode;

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

    private Holding(CreateHoldingCommand command) {
        validate(command);
        this.accountId = command.accountId();
        this.holdingName = command.holdingName();
        this.seasonId = command.seasonId();
        this.seasonNumber = command.seasonNumber();
        this.instrumentCode = InstrumentCode.of(command.instrumentCode());
        this.quantity = command.quantity();
        this.averageBuyPrice = command.averageBuyPrice();
        this.currentPrice = command.currentPrice();
        this.totalBuyAmount = command.quantity() * command.averageBuyPrice();
        recalculateDerived();
    }

    public static Holding create(CreateHoldingCommand command) {
        return new Holding(command);
    }

    public void addBuy(long buyQuantity, long buyPrice) {
        if (buyQuantity <= 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_BUY_QUANTITY);
        }
        if (buyPrice <= 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_BUY_PRICE);
        }
        this.totalBuyAmount += buyQuantity * buyPrice;
        this.quantity += buyQuantity;
        this.averageBuyPrice = Math.round((double) this.totalBuyAmount / this.quantity);
        recalculateDerived();
    }

    public void sell(long sellQuantity) {
        if (sellQuantity <= 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_SELL_QUANTITY);
        }
        if (sellQuantity > this.quantity) {
            throw new HoldingDomainException(HoldingErrorCode.EXCEED_SELL_QUANTITY);
        }
        this.quantity -= sellQuantity;
        this.totalBuyAmount = this.averageBuyPrice * this.quantity;
        recalculateDerived();
    }

    public void updateCurrentPrice(long currentPrice) {
        if (currentPrice <= 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_UPDATE_PRICE);
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

    private static void validate(CreateHoldingCommand command) {
        if (command.accountId() == null) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_ACCOUNT_ID);
        }
        if (command.holdingName() == null || command.holdingName().isBlank()) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_HOLDING_NAME);
        }
        if (command.seasonId() == null) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_SEASON_ID);
        }
        if (command.seasonNumber() <= 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_SEASON_NUMBER);
        }
        if (command.instrumentCode() == null || command.instrumentCode().isBlank()) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_INSTRUMENT_CODE);
        }
        if (command.quantity() < 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_QUANTITY);
        }
        if (command.averageBuyPrice() < 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_AVERAGE_BUY_PRICE);
        }
        if (command.currentPrice() <= 0) {
            throw new HoldingDomainException(HoldingErrorCode.INVALID_CURRENT_PRICE);
        }
    }
}