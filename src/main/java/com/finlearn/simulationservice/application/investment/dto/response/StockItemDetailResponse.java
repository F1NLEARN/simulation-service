package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockItemDetailResponse(
        UUID id,
        String stockCode,
        String name,
        StockAssetType assetType,
        Long currentPrice,
        StockPriceSource currentPriceSource,
        LocalDateTime currentPriceUpdatedAt,
        boolean tradable
) {
    public static StockItemDetailResponse from(StockItem stockItem) {
        return from(stockItem, stockItem.getCurrentPrice(), stockItem.getCurrentPrice() == null ? null : StockPriceSource.DB_CACHE);
    }

    public static StockItemDetailResponse from(StockItem stockItem, Long currentPrice, StockPriceSource currentPriceSource) {
        return new StockItemDetailResponse(
                stockItem.getStockItemId(),
                stockItem.getStockCode(),
                stockItem.getStockName(),
                stockItem.getAssetType(),
                currentPrice,
                currentPriceSource,
                stockItem.getCurrentPriceUpdatedAt(),
                currentPrice != null && currentPrice > 0
        );
    }
}
