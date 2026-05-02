package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;

public record StockItemResponse(
        String stockName,
        String stockCode,
        StockAssetType assetType
) {
    public static StockItemResponse from(StockItem stockItem) {
        return new StockItemResponse(
                stockItem.getStockName(),
                stockItem.getStockCode(),
                stockItem.getAssetType()
        );
    }
}
