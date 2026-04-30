package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.math.BigDecimal;
import java.util.UUID;

public record StockItemDetailResponse(
        UUID id,
        String stockCode,
        String symbol,
        String name,
        StockAssetType assetType,
        BigDecimal currentPrice,
        boolean tradable
) {
    public static StockItemDetailResponse from(StockItem stockItem) {
        return new StockItemDetailResponse(
                stockItem.getStockItemId(),
                stockItem.getStockCode(),
                stockItem.getStockCode(),
                stockItem.getStockName(),
                stockItem.getAssetType(),
                stockItem.getCurrentPrice(),
                stockItem.getCurrentPrice() != null && stockItem.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0
        );
    }
}
