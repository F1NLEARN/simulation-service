package com.finlearn.simulationservice.application.investment.dto.request;

import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.util.UUID;

public record SellStockRequest(
        UUID investmentAccountId,
        StockAssetType assetType,
        String symbol,
        long quantity
) {
}
