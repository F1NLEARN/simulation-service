package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.domain.analysis.service.PortfolioAnalysisDomainService;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisQueryService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioAnalysisDomainService portfolioAnalysisDomainService;

    public PortfolioAnalysisResponse getPortfolioAnalysis(GetPortfolioAnalysisQuery query) {
        InvestmentAccount account = investmentAccountRepository
                .findByParticipant_InvestorIdAndStatus(query.investorId(), InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        List<Holding> holdings = holdingRepository.findAllWithFilter(account.getAccountId(), null);

        long totalValuationAmount = holdings.stream().mapToLong(Holding::getValuationAmount).sum();
        BigDecimal topHoldingWeight = holdings.stream()
                .map(h -> totalValuationAmount == 0
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(h.getValuationAmount())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalValuationAmount), 2, RoundingMode.HALF_UP))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        long totalAssetAmount = account.getTotalAssetAmount();
        BigDecimal cashWeight = totalAssetAmount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(account.getCurrentCashBalance())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAssetAmount), 2, RoundingMode.HALF_UP);

        PortfolioDiagnosis diagnosis = portfolioAnalysisDomainService.diagnose(
                topHoldingWeight, holdings.size(), cashWeight, account.getTotalReturnRate());

        return PortfolioAnalysisResponse.of(account, holdings, diagnosis);
    }
}
