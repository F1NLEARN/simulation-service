package com.finlearn.simulationservice.application.analysis.dto.response;

import java.math.BigDecimal;

public record PortfolioAllocationResponse(
        BigDecimal stockWeight,
        BigDecimal etfWeight,
        BigDecimal cashWeight,
        BigDecimal topHoldingWeight,
        int holdingCount
) {}
