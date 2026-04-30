package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "investment_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID investmentAccountId;

    @Column
    private UUID seasonParticipantId;

    @Column(length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestmentAccountStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal seedMoney;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cashBalance;

    @Transient
    private final List<HoldingStock> holdingStocks = new ArrayList<>();

    @Transient
    private final List<StockTransaction> stockTransactions = new ArrayList<>();

    private InvestmentAccount(UUID seasonParticipantId, BigDecimal seedMoney) {
        if (seedMoney == null || seedMoney.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_SEED_MONEY);
        }
        this.seasonParticipantId = seasonParticipantId;
        this.seedMoney = seedMoney;
        this.cashBalance = seedMoney;
        this.status = InvestmentAccountStatus.ACTIVE;
    }

    public static InvestmentAccount open(UUID seasonParticipantId, BigDecimal seedMoney) {
        return new InvestmentAccount(seasonParticipantId, seedMoney);
    }

    public static InvestmentAccount openForUser(String userId, BigDecimal seedMoney) {
        InvestmentAccount account = new InvestmentAccount(null, seedMoney);
        account.userId = userId;
        return account;
    }

    public void close() {
        this.status = InvestmentAccountStatus.CLOSED;
    }

    public void addCashBalance(BigDecimal amount) {
        ensureActive();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_STOCK_PRICE);
        }
        this.cashBalance = this.cashBalance.add(amount);
    }

    public StockTransaction buy(StockAssetType assetType, String symbol, long quantity, BigDecimal unitPrice, LocalDateTime executedAt) {
        ensureActive();
        validateOrder(quantity, unitPrice);

        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (cashBalance.compareTo(totalAmount) < 0) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_CASH);
        }

        HoldingStock holding = findHolding(assetType, symbol);
        if (holding == null) {
            holding = new HoldingStock(assetType, normalizeSymbol(symbol), quantity);
            this.holdingStocks.add(holding);
        } else {
            holding.addQuantity(quantity);
        }

        this.cashBalance = this.cashBalance.subtract(totalAmount);

        StockTransaction tx = StockTransaction.buy(
                assetType,
                normalizeSymbol(symbol),
                quantity,
                unitPrice,
                executedAt == null ? LocalDateTime.now() : executedAt
        );
        this.stockTransactions.add(tx);
        return tx;
    }

    public StockTransaction sell(StockAssetType assetType, String symbol, long quantity, BigDecimal unitPrice, LocalDateTime executedAt) {
        ensureActive();
        validateOrder(quantity, unitPrice);

        HoldingStock holding = findHolding(assetType, symbol);
        if (holding == null) {
            throw new InvestmentException(InvestmentErrorCode.HOLDING_STOCK_NOT_FOUND);
        }
        if (holding.getQuantity() < quantity) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_HOLDING_QUANTITY);
        }

        holding.subtractQuantity(quantity);
        if (holding.getQuantity() == 0) {
            this.holdingStocks.remove(holding);
        }

        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.cashBalance = this.cashBalance.add(totalAmount);

        StockTransaction tx = StockTransaction.sell(
                assetType,
                normalizeSymbol(symbol),
                quantity,
                unitPrice,
                executedAt == null ? LocalDateTime.now() : executedAt
        );
        this.stockTransactions.add(tx);
        return tx;
    }

    private void ensureActive() {
        if (this.status != InvestmentAccountStatus.ACTIVE) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ACCOUNT_STATUS);
        }
    }

    private void validateOrder(long quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_STOCK_PRICE);
        }
    }

    private HoldingStock findHolding(StockAssetType assetType, String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        return this.holdingStocks.stream()
                .filter(h -> h.getAssetType() == assetType && h.getSymbol().equals(normalizedSymbol))
                .findFirst()
                .orElse(null);
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase();
    }

    @Getter
    public static class HoldingStock {
        private final StockAssetType assetType;
        private final String symbol;
        private long quantity;

        public HoldingStock(StockAssetType assetType, String symbol, long quantity) {
            this.assetType = assetType;
            this.symbol = symbol;
            this.quantity = quantity;
        }

        public void addQuantity(long quantity) {
            this.quantity += quantity;
        }

        public void subtractQuantity(long quantity) {
            this.quantity -= quantity;
        }
    }
}
