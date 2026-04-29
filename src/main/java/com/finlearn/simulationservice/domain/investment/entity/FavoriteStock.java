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
        name = "favorite_stocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_favorite_stock_user_symbol", columnNames = {"user_id", "symbol"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID favoriteStockId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StockAssetType assetType;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Builder
    private FavoriteStock(UUID userId, StockAssetType assetType, String symbol) {
        this.userId = userId;
        this.assetType = assetType;
        this.symbol = symbol;
    }

    public static FavoriteStock register(UUID userId, StockAssetType assetType, String symbol) {
        return FavoriteStock.builder()
                .userId(userId)
                .assetType(assetType)
                .symbol(symbol)
                .build();
    }
}
