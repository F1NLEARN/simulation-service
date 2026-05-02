package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
        name = "season_investment_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_investment_account_investor_season", columnNames = {"investor_id", "season_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id")
    private UUID accountId;

    @Embedded
    private SeasonParticipant participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private InvestmentAccountStatus status;

    @Column(name = "initial_seed_money", nullable = false)
    private long initialSeedMoney;

    @Column(name = "current_cash_balance", nullable = false)
    private long currentCashBalance;

    @Column(name = "total_valuation_amount", nullable = false)
    private long totalValuationAmount;

    @Column(name = "total_asset_amount", nullable = false)
    private long totalAssetAmount;

    @Column(name = "realized_profit_loss", nullable = false)
    private long realizedProfitLoss;

    @Column(name = "unrealized_profit_loss", nullable = false)
    private long unrealizedProfitLoss;

    @Column(name = "total_return_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalReturnRate;

    @OneToMany(mappedBy = "investmentAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<HoldingStock> holdingStocks = new ArrayList<>();

    @OneToMany(mappedBy = "investmentAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<StockTransaction> stockTransactions = new ArrayList<>();

    @Builder
    private InvestmentAccount(SeasonParticipant participant, InvestmentAccountStatus status,
                              long initialSeedMoney, long currentCashBalance, long totalValuationAmount,
                              long totalAssetAmount, long realizedProfitLoss, long unrealizedProfitLoss,
                              BigDecimal totalReturnRate) {
        this.participant = participant;
        this.status = status;
        this.initialSeedMoney = initialSeedMoney;
        this.currentCashBalance = currentCashBalance;
        this.totalValuationAmount = totalValuationAmount;
        this.totalAssetAmount = totalAssetAmount;
        this.realizedProfitLoss = realizedProfitLoss;
        this.unrealizedProfitLoss = unrealizedProfitLoss;
        this.totalReturnRate = totalReturnRate;
    }

    public static InvestmentAccount open(SeasonParticipant participant, long seedMoney) {
        validateSeedMoney(seedMoney);
        return InvestmentAccount.builder()
                .participant(participant)
                .status(InvestmentAccountStatus.ACTIVE)
                .initialSeedMoney(seedMoney)
                .currentCashBalance(seedMoney)
                .totalValuationAmount(0L)
                .totalAssetAmount(seedMoney)
                .realizedProfitLoss(0L)
                .unrealizedProfitLoss(0L)
                .totalReturnRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    public StockTransaction buy(String instrumentCode, String holdingName, long quantity, long tradePrice,
                                LocalDateTime tradeAt) {
        ensureTradable();
        long totalOrderAmount = calculateOrderAmount(quantity, tradePrice);
        if (this.currentCashBalance < totalOrderAmount) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_CASH);
        }

        Optional<HoldingStock> existingHolding = findHolding(instrumentCode);
        if (existingHolding.isPresent()) {
            existingHolding.get().buy(quantity, tradePrice);
        } else {
            HoldingStock created = HoldingStock.open(this, instrumentCode, holdingName,
                    participant.getSeasonId(), participant.getSeasonNumber(), quantity, tradePrice);
            this.holdingStocks.add(created);
        }

        this.currentCashBalance -= totalOrderAmount;

        StockTransaction transaction = StockTransaction.buy(this, participant.getSeasonId(),
                participant.getSeasonNumber(), instrumentCode, quantity, tradePrice,
                this.currentCashBalance, tradeAt);
        this.stockTransactions.add(transaction);

        recalculateAccountSummary();
        return transaction;
    }

    public StockTransaction sell(String instrumentCode, long quantity, long tradePrice, LocalDateTime tradeAt) {
        ensureTradable();
        calculateOrderAmount(quantity, tradePrice);

        HoldingStock holdingStock = findHolding(instrumentCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.HOLDING_STOCK_NOT_FOUND));

        long realizedProfit = (tradePrice - holdingStock.getAverageBuyPrice()) * quantity;
        holdingStock.sell(quantity, tradePrice);

        this.currentCashBalance += tradePrice * quantity;
        this.realizedProfitLoss += realizedProfit;

        if (holdingStock.isEmpty()) {
            this.holdingStocks.remove(holdingStock);
        }

        StockTransaction transaction = StockTransaction.sell(this, participant.getSeasonId(),
                participant.getSeasonNumber(), instrumentCode, quantity, tradePrice,
                this.currentCashBalance, tradeAt);
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

    private Optional<HoldingStock> findHolding(String instrumentCode) {
        return this.holdingStocks.stream()
                .filter(h -> h.isSameStock(instrumentCode))
                .findFirst();
    }

    private long calculateOrderAmount(long quantity, long tradePrice) {
        if (quantity <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }
        if (tradePrice <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_STOCK_PRICE);
        }
        return tradePrice * quantity;
    }

    private void recalculateAccountSummary() {
        this.totalValuationAmount = this.holdingStocks.stream()
                .mapToLong(HoldingStock::getValuationAmount)
                .sum();
        this.totalAssetAmount = this.currentCashBalance + this.totalValuationAmount;
        this.unrealizedProfitLoss = this.holdingStocks.stream()
                .mapToLong(HoldingStock::getUnrealizedProfitLoss)
                .sum();

        if (this.initialSeedMoney == 0) {
            this.totalReturnRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }
        this.totalReturnRate = BigDecimal.valueOf(this.totalAssetAmount - this.initialSeedMoney)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(this.initialSeedMoney), 2, RoundingMode.HALF_UP);
    }

    private static void validateSeedMoney(long seedMoney) {
        if (seedMoney < 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_SEED_MONEY);
        }
    }
}
