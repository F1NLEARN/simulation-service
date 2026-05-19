package com.finlearn.simulationservice.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioSnapshotEvent(
        UUID userId,
        UUID accountId,
        UUID seasonId,
        int seasonNumber,
        String userNickname,
        BigDecimal overallReturnRate,
        BigDecimal stockReturnRate,
        BigDecimal etfReturnRate,
        int stockHoldingCount,
        int etfHoldingCount,
        LocalDateTime updatedAt
) {}