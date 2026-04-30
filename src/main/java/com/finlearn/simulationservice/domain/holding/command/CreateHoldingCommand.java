package com.finlearn.simulationservice.domain.holding.command;

import java.util.UUID;

public record CreateHoldingCommand(
        UUID accountId,
        String holdingName,
        UUID seasonId,
        int seasonNumber,
        String instrumentCode,
        long quantity,
        long averageBuyPrice,
        long currentPrice
) {}