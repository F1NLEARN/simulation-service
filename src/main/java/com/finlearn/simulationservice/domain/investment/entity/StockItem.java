package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "stock_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_item_code", columnNames = "stock_code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID stockItemId;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StockAssetType assetType;

    @Column(name = "current_price")
    private Long currentPrice;

    @Builder
    private StockItem(String stockName, String stockCode, StockAssetType assetType, Long currentPrice) {
        this.stockName = stockName;
        this.stockCode = stockCode;
        this.assetType = assetType;
        this.currentPrice = currentPrice;
    }

    public static StockItem create(String stockName, String stockCode, StockAssetType assetType) {
        return create(stockName, stockCode, assetType, null);
    }

    public static StockItem create(String stockName, String stockCode, StockAssetType assetType, Long currentPrice) {
        return StockItem.builder()
                .stockName(stockName)
                .stockCode(stockCode)
                .assetType(assetType)
                .currentPrice(currentPrice)
                .build();
    }
}
