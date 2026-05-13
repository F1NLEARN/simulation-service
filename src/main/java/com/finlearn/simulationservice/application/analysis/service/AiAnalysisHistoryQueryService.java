package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisHistoryResponse;
import com.finlearn.simulationservice.application.analysis.query.GetAiAnalysisHistoryQuery;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
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
public class AiAnalysisHistoryQueryService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final AiAnalysisRepository aiAnalysisRepository;

    public List<AiAnalysisHistoryResponse> getHistory(GetAiAnalysisHistoryQuery query) {
        InvestmentAccount account = investmentAccountRepository
                .findByParticipant_InvestorIdAndStatus(query.investorId(), InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        return aiAnalysisRepository
                .findAllByAccountIdOrderByAnalyzedAtDesc(account.getAccountId())
                .stream()
                .map(AiAnalysisHistoryResponse::from)
                .toList();
    }
}
