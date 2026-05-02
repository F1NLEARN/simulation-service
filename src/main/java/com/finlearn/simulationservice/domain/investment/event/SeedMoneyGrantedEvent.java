package com.finlearn.simulationservice.domain.investment.event;

import java.util.UUID;

public record SeedMoneyGrantedEvent(
        UUID seasonId,
        UUID investorId,
        long seedMoney
) {
}
