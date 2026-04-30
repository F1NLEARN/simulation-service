package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTransaction extends BaseEntity {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID stockTransactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_account_id", nullable = false)
    private InvestmentAccount investmentAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StockTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StockAssetType assetType;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, precision = 19, scale = MONEY_SCALE)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = MONEY_SCALE)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    @Builder
    private StockTransaction(InvestmentAccount investmentAccount, StockTransactionType transactionType,
                             StockAssetType assetType, String symbol, long quantity, BigDecimal unitPrice,
                             BigDecimal totalAmount, LocalDateTime executedAt) {
        this.investmentAccount = investmentAccount;
        this.transactionType = transactionType;
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.executedAt = executedAt;
    }

    public static StockTransaction buy(InvestmentAccount account, StockAssetType assetType, String symbol,
                                       long quantity, BigDecimal unitPrice, LocalDateTime executedAt) {
        return create(account, StockTransactionType.BUY, assetType, symbol, quantity, unitPrice, executedAt);
    }

    public static StockTransaction sell(InvestmentAccount account, StockAssetType assetType, String symbol,
                                        long quantity, BigDecimal unitPrice, LocalDateTime executedAt) {
        return create(account, StockTransactionType.SELL, assetType, symbol, quantity, unitPrice, executedAt);
    }

    private static StockTransaction create(InvestmentAccount account, StockTransactionType transactionType,
                                           StockAssetType assetType, String symbol, long quantity,
                                           BigDecimal unitPrice, LocalDateTime executedAt) {
        validateQuantity(quantity);
        validatePrice(unitPrice);
        LocalDateTime at = executedAt == null ? LocalDateTime.now() : executedAt;
        BigDecimal scaledPrice = unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return StockTransaction.builder()
                .investmentAccount(account)
                .transactionType(transactionType)
                .assetType(assetType)
                .symbol(symbol)
                .quantity(quantity)
                .unitPrice(scaledPrice)
                .totalAmount(scaledPrice.multiply(BigDecimal.valueOf(quantity)).setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .executedAt(at)
                .build();
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
}
