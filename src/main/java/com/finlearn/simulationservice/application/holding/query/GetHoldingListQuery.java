package com.finlearn.simulationservice.application.holding.query;

import java.util.UUID;

public record GetHoldingListQuery(
        UUID accountId,
        String instrumentCode
) {
}