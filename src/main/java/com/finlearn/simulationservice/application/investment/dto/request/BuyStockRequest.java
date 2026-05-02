package com.finlearn.simulationservice.application.investment.dto.request;

import java.util.UUID;

public record BuyStockRequest(
        UUID accountId,
        String instrumentCode,
        long quantity
) {
}
