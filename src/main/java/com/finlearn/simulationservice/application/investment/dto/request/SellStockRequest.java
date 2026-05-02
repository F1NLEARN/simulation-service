package com.finlearn.simulationservice.application.investment.dto.request;

import java.util.UUID;

public record SellStockRequest(
        UUID accountId,
        String instrumentCode,
        long quantity
) {
}
