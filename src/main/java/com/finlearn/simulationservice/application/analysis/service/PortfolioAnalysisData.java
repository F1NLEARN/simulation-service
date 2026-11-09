package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAllocationResponse;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAnalysisData(
        InvestmentAccount account,
        List<Holding> holdings,
        PortfolioAllocationResponse allocation,
        PortfolioDiagnosis diagnosis,
        BigDecimal stockReturnRate,
        BigDecimal etfReturnRate
) {
}
