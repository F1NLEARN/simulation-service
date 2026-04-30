package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "holding_stocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_holding_account_symbol", columnNames = {"investment_account_id", "symbol"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldingStock extends BaseEntity {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID holdingStockId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_account_id", nullable = false)
    private InvestmentAccount investmentAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StockAssetType assetType;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "average_buy_price", nullable = false, precision = 19, scale = MONEY_SCALE)
    private BigDecimal averageBuyPrice;

    @Builder
    private HoldingStock(InvestmentAccount investmentAccount, StockAssetType assetType, String symbol, long quantity,
                         BigDecimal averageBuyPrice) {
        this.investmentAccount = investmentAccount;
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
    }

    public static HoldingStock open(InvestmentAccount account, StockAssetType assetType, String symbol,
                                    long quantity, BigDecimal unitPrice) {
        validateQuantity(quantity);
        validatePrice(unitPrice);
        return HoldingStock.builder()
                .investmentAccount(account)
                .assetType(assetType)
                .symbol(symbol)
                .quantity(quantity)
                .averageBuyPrice(scale(unitPrice))
                .build();
    }

    public void buy(long additionalQuantity, BigDecimal unitPrice) {
        validateQuantity(additionalQuantity);
        validatePrice(unitPrice);

        BigDecimal additionalAmount = scale(unitPrice).multiply(BigDecimal.valueOf(additionalQuantity));
        BigDecimal currentAmount = getBookAmount();
        long nextQuantity = this.quantity + additionalQuantity;

        this.averageBuyPrice = currentAmount
                .add(additionalAmount)
                .divide(BigDecimal.valueOf(nextQuantity), MONEY_SCALE, RoundingMode.HALF_UP);
        this.quantity = nextQuantity;
    }

    public void sell(long sellQuantity) {
        validateQuantity(sellQuantity);
        if (this.quantity < sellQuantity) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_HOLDING_QUANTITY);
        }

        this.quantity -= sellQuantity;
        if (this.quantity == 0) {
            this.averageBuyPrice = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
    }

    public boolean isSameStock(StockAssetType stockAssetType, String stockSymbol) {
        return this.assetType == stockAssetType && Objects.equals(this.symbol, stockSymbol);
    }

    public boolean isEmpty() {
        return this.quantity == 0;
    }

    public BigDecimal getBookAmount() {
        return this.averageBuyPrice.multiply(BigDecimal.valueOf(this.quantity)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static void validateQuantity(long quantity) {
        if (quantity <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }
    }

    private static void validatePrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_STOCK_PRICE);
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
