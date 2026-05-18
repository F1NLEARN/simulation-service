package com.finlearn.simulationservice.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradeExecutedEvent(
        UUID userId,
        UUID accountId,
        UUID seasonId,
        String tradeType,
        String assetType,
        String stockCode,
        LocalDateTime executedAt
) {}