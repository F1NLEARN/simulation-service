package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.FavoriteStock;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.util.UUID;

public record FavoriteStockResponse(
        UUID favoriteStockId,
        StockAssetType assetType,
        String symbol
) {
    public static FavoriteStockResponse from(FavoriteStock favoriteStock) {
        return new FavoriteStockResponse(
                favoriteStock.getFavoriteStockId(),
                favoriteStock.getAssetType(),
                favoriteStock.getSymbol()
        );
    }
}
