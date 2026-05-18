package com.finlearn.simulationservice.domain.tradehistory.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradeCompletedEvent(
        UUID userId,
        UUID accountId,
        UUID seasonId,
        String tradeType,
        String assetType,
        String stockCode,
        LocalDateTime executedAt
) {}