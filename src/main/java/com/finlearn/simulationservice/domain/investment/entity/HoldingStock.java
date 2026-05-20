package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "holding_stocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_holding_account_instrument", columnNames = {"account_id", "instrument_code"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldingStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "holding_id")
    private UUID holdingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount investmentAccount;

    @Column(name = "holding_name", nullable = false, length = 50)
    private String holdingName;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "season_number", nullable = false)
    private int seasonNumber;

    @Column(name = "instrument_code", nullable = false, length = 20)
    private String instrumentCode;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "average_buy_price", nullable = false)
    private long averageBuyPrice;

    @Column(name = "current_price", nullable = false)
    private long currentPrice;

    @Column(name = "total_buy_amount", nullable = false)
    private long totalBuyAmount;

    @Column(name = "valuation_amount", nullable = false)
    private long valuationAmount;

    @Column(name = "unrealized_profit_loss", nullable = false)
    private long unrealizedProfitLoss;

    @Column(name = "return_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal returnRate;

    @Builder
    private HoldingStock(InvestmentAccount investmentAccount, String holdingName, UUID seasonId,
                         int seasonNumber, String instrumentCode, long quantity, long averageBuyPrice,
                         long currentPrice) {
        this.investmentAccount = investmentAccount;
        this.holdingName = holdingName;
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.instrumentCode = instrumentCode;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.currentPrice = currentPrice;
    }

    public static HoldingStock open(InvestmentAccount account, String instrumentCode, String holdingName,
                                    UUID seasonId, int seasonNumber, long quantity, long unitPrice) {
        validateQuantity(quantity);
        validatePrice(unitPrice);
        HoldingStock stock = HoldingStock.builder()
                .investmentAccount(account)
                .instrumentCode(instrumentCode)
                .holdingName(holdingName)
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .quantity(quantity)
                .averageBuyPrice(unitPrice)
                .currentPrice(unitPrice)
                .build();
        stock.recalculateMetrics();
        return stock;
    }

    public void buy(long additionalQuantity, long unitPrice) {
        validateQuantity(additionalQuantity);
        validatePrice(unitPrice);

        long currentTotalAmount = this.averageBuyPrice * this.quantity;
        long additionalAmount = unitPrice * additionalQuantity;
        long nextQuantity = this.quantity + additionalQuantity;

        this.averageBuyPrice = (currentTotalAmount + additionalAmount) / nextQuantity;
        this.quantity = nextQuantity;
        this.currentPrice = unitPrice;

        recalculateMetrics();
    }

    public void sell(long sellQuantity, long unitPrice) {
        validateQuantity(sellQuantity);
        if (this.quantity < sellQuantity) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_HOLDING_QUANTITY);
        }
        this.quantity -= sellQuantity;
        this.currentPrice = unitPrice;
        if (this.quantity == 0) {
            this.averageBuyPrice = 0L;
        }
        recalculateMetrics();
    }

    public boolean isSameStock(String code) {
        return this.instrumentCode.equals(code);
    }

    public boolean isEmpty() {
        return this.quantity == 0;
    }

    private void recalculateMetrics() {
        this.totalBuyAmount = this.averageBuyPrice * this.quantity;
        this.valuationAmount = this.currentPrice * this.quantity;
        this.unrealizedProfitLoss = this.valuationAmount - this.totalBuyAmount;

        if (this.averageBuyPrice == 0 || this.quantity == 0) {
            this.returnRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.returnRate = BigDecimal.valueOf(this.currentPrice - this.averageBuyPrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(this.averageBuyPrice), 2, RoundingMode.HALF_UP);
        }
    }

    private static void validateQuantity(long quantity) {
        if (quantity <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }
    }

    private static void validatePrice(long unitPrice) {
        if (unitPrice <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_STOCK_PRICE);
        }
    }
}