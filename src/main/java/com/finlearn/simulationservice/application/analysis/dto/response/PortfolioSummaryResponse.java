package com.finlearn.simulationservice.application.analysis.dto.response;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(
        long totalBuyAmount,
        long totalValuationAmount,
        long cashBalance,
        long totalAssetAmount,
        long totalProfitLoss,
        BigDecimal totalReturnRate,
        BigDecimal stockReturnRate,
        BigDecimal etfReturnRate
) {}