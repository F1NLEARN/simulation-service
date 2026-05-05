package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisQueryService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final HoldingRepository holdingRepository;

    public PortfolioAnalysisResponse getPortfolioAnalysis(GetPortfolioAnalysisQuery query) {
        InvestmentAccount account = investmentAccountRepository
                .findByParticipant_InvestorIdAndStatus(query.investorId(), InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        List<Holding> holdings = holdingRepository.findAllWithFilter(account.getAccountId(), null);

        return PortfolioAnalysisResponse.of(account.getAccountId(), holdings);
    }
}
