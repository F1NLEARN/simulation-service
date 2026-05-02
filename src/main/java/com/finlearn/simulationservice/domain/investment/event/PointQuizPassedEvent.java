package com.finlearn.simulationservice.domain.investment.event;

import java.util.UUID;

public record PointQuizPassedEvent(
        UUID seasonId,
        int seasonNumber,
        UUID investorId,
        String investorName,
        long seedMoney
) {
}
