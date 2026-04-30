package com.finlearn.simulationservice.domain.investment.event;

import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockBoughtEvent(
        UUID investmentAccountId,
        UUID seasonParticipantId,
        StockAssetType assetType,
        String symbol,
        long quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        LocalDateTime executedAt
) {
}
