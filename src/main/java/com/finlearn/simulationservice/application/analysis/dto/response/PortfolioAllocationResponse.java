package com.finlearn.simulationservice.application.analysis.dto.response;

import java.math.BigDecimal;

public record PortfolioAllocationResponse(
        BigDecimal stockWeight,   // 5순위(ETF 자산군 비중 계산) 구현 후 채워질 예정
        BigDecimal etfWeight,     // 5순위(ETF 자산군 비중 계산) 구현 후 채워질 예정
        BigDecimal cashWeight,
        BigDecimal topHoldingWeight,
        int holdingCount
) {}
