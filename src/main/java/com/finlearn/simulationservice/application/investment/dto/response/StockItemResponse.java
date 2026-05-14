package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockItemResponse(
        UUID id,
        String stockCode,
        String name,
        StockAssetType assetType,
        Long currentPrice,
        StockPriceSource currentPriceSource,
        LocalDateTime currentPriceUpdatedAt,
        boolean tradable
) {
    public static StockItemResponse from(StockItem stockItem) {
        return new StockItemResponse(
                stockItem.getStockItemId(),
                stockItem.getStockCode(),
                stockItem.getStockName(),
                stockItem.getAssetType(),
                stockItem.getCurrentPrice(),
                stockItem.getCurrentPrice() == null ? null : StockPriceSource.DB_CACHE,
                stockItem.getCurrentPriceUpdatedAt(),
                stockItem.getCurrentPrice() != null && stockItem.getCurrentPrice() > 0
        );
    }
}
