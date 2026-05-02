package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.FavoriteStock;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.util.UUID;

public record FavoriteStockResponse(
        UUID favoriteStockId,
        StockAssetType assetType,
        String symbol,
        String stockName
) {
    public static FavoriteStockResponse from(FavoriteStock favoriteStock, String stockName) {
        return new FavoriteStockResponse(
                favoriteStock.getFavoriteStockId(),
                favoriteStock.getAssetType(),
                favoriteStock.getSymbol(),
                stockName
        );
    }
}
