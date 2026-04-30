package com.finlearn.simulationservice.domain.investment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PointQuizPassedEvent(
        UUID seasonId,
        UUID userId,
        BigDecimal seedMoney
) {
}
