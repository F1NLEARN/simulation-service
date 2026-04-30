package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class StockTransaction {

    private final StockAssetType assetType;
    private final String symbol;
    private final long quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final LocalDateTime executedAt;

    private StockTransaction(
            StockAssetType assetType,
            String symbol,
            long quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            LocalDateTime executedAt
    ) {
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.executedAt = executedAt;
    }

    public static StockTransaction buy(StockAssetType assetType, String symbol, long quantity, BigDecimal unitPrice, LocalDateTime executedAt) {
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new StockTransaction(assetType, symbol, quantity, unitPrice, totalAmount, executedAt);
    }

    public static StockTransaction sell(StockAssetType assetType, String symbol, long quantity, BigDecimal unitPrice, LocalDateTime executedAt) {
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new StockTransaction(assetType, symbol, quantity, unitPrice, totalAmount, executedAt);
    }
}
