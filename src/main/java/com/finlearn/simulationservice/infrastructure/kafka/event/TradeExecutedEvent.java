package com.finlearn.simulationservice.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradeExecutedEvent(
        UUID userId,
        UUID accountId,
        UUID seasonId,
        int seasonNumber,
        String tradeType,
        String assetType,
        String stockCode,
        int holdCount,
        double returnRate,
        String userNickname,
        LocalDateTime executedAt
) {}