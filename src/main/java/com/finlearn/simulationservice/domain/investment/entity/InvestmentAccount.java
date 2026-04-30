package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "investment_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_investment_account_participant", columnNames = "season_participant_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentAccount extends BaseEntity {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID investmentAccountId;

    @Column(name = "season_participant_id", nullable = false)
    private UUID seasonParticipantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestmentAccountStatus status;

    @Column(name = "initial_seed_money", nullable = false, precision = 19, scale = MONEY_SCALE)
    private BigDecimal initialSeedMoney;

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = MONEY_SCALE)
    private BigDecimal cashBalance;

    @Column(name = "total_evaluated_amount", nullable = false, precision = 19, scale = MONEY_SCALE)
    private BigDecimal totalEvaluatedAmount;

    @Column(name = "total_profit_loss", nullable = false, precision = 19, scale = MONEY_SCALE)
    private BigDecimal totalProfitLoss;

    @Column(name = "total_return_rate", nullable = false, precision = 10, scale = RATE_SCALE)
    private BigDecimal totalReturnRate;

    @OneToMany(mappedBy = "investmentAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<HoldingStock> holdingStocks = new ArrayList<>();

    @OneToMany(mappedBy = "investmentAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<StockTransaction> stockTransactions = new ArrayList<>();

    @Builder
    private InvestmentAccount(UUID seasonParticipantId, InvestmentAccountStatus status, BigDecimal initialSeedMoney,
                              BigDecimal cashBalance, BigDecimal totalEvaluatedAmount, BigDecimal totalProfitLoss,
                              BigDecimal totalReturnRate) {
        this.seasonParticipantId = seasonParticipantId;
        this.status = status;
        this.initialSeedMoney = initialSeedMoney;
        this.cashBalance = cashBalance;
        this.totalEvaluatedAmount = totalEvaluatedAmount;
        this.totalProfitLoss = totalProfitLoss;
        this.totalReturnRate = totalReturnRate;
    }

    public static InvestmentAccount open(UUID seasonParticipantId, BigDecimal seedMoney) {
        validateSeedMoney(seedMoney);
        BigDecimal scaledSeedMoney = scale(seedMoney);
        return InvestmentAccount.builder()
                .seasonParticipantId(seasonParticipantId)
                .status(InvestmentAccountStatus.ACTIVE)
                .initialSeedMoney(scaledSeedMoney)
                .cashBalance(scaledSeedMoney)
                .totalEvaluatedAmount(scaledSeedMoney)
                .totalProfitLoss(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .totalReturnRate(BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP))
                .build();
    }

    public StockTransaction buy(StockAssetType assetType, String symbol, long quantity, BigDecimal unitPrice,
                                LocalDateTime executedAt) {
        ensureTradable();
        BigDecimal totalOrderAmount = calculateOrderAmount(quantity, unitPrice);
        if (this.cashBalance.compareTo(totalOrderAmount) < 0) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_CASH);
        }

        Optional<HoldingStock> existingHolding = findHolding(assetType, symbol);
        if (existingHolding.isPresent()) {
            existingHolding.get().buy(quantity, unitPrice);
        } else {
            HoldingStock created = HoldingStock.open(this, assetType, symbol, quantity, unitPrice);
            this.holdingStocks.add(created);
        }

        this.cashBalance = this.cashBalance.subtract(totalOrderAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        StockTransaction transaction = StockTransaction.buy(this, assetType, symbol, quantity, unitPrice, executedAt);
        this.stockTransactions.add(transaction);
        recalculateAccountSummary();
        return transaction;
    }

    public StockTransaction sell(StockAssetType assetType, String symbol, long quantity, BigDecimal unitPrice,
                                 LocalDateTime executedAt) {
        ensureTradable();
        calculateOrderAmount(quantity, unitPrice);

        HoldingStock holdingStock = findHolding(assetType, symbol)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.HOLDING_STOCK_NOT_FOUND));
        holdingStock.sell(quantity);

        BigDecimal proceeds = scale(unitPrice).multiply(BigDecimal.valueOf(quantity)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.cashBalance = this.cashBalance.add(proceeds).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (holdingStock.isEmpty()) {
            this.holdingStocks.remove(holdingStock);
        }

        StockTransaction transaction = StockTransaction.sell(this, assetType, symbol, quantity, unitPrice, executedAt);
        this.stockTransactions.add(transaction);
        recalculateAccountSummary();
        return transaction;
    }

    public void close() {
        if (this.status != InvestmentAccountStatus.ACTIVE) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ACCOUNT_STATUS);
        }
        this.status = InvestmentAccountStatus.CLOSED;
    }

    private void ensureTradable() {
        if (this.status != InvestmentAccountStatus.ACTIVE) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ACCOUNT_STATUS);
        }
    }

    private Optional<HoldingStock> findHolding(StockAssetType assetType, String symbol) {
        return this.holdingStocks.stream()
                .filter(holdingStock -> holdingStock.isSameStock(assetType, symbol))
                .findFirst();
    }

    private BigDecimal calculateOrderAmount(long quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_STOCK_PRICE);
        }

        return scale(unitPrice)
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void recalculateAccountSummary() {
        BigDecimal holdingBookAmount = this.holdingStocks.stream()
                .map(HoldingStock::getBookAmount)
                .reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add);

        this.totalEvaluatedAmount = this.cashBalance.add(holdingBookAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalProfitLoss = this.totalEvaluatedAmount.subtract(this.initialSeedMoney).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (this.initialSeedMoney.compareTo(BigDecimal.ZERO) == 0) {
            this.totalReturnRate = BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
            return;
        }

        this.totalReturnRate = this.totalProfitLoss
                .divide(this.initialSeedMoney, RATE_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static void validateSeedMoney(BigDecimal seedMoney) {
        if (seedMoney == null || seedMoney.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_SEED_MONEY);
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
