package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.response.InvestmentAccountResponse;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentAccountService {

    // TODO: Season 도메인 연동 후 실제 seasonId/seasonNumber/investorName를 조회하도록 변경
    private static final UUID MVP_DEFAULT_SEASON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int MVP_DEFAULT_SEASON_NUMBER = 1;
    private static final long DEFAULT_INITIAL_SEED_MONEY = 10_000_000L;

    private final InvestmentAccountRepository investmentAccountRepository;

    @Transactional
    public InvestmentAccountResponse createAccount(UUID userId) {
        investmentAccountRepository.findByParticipant_InvestorIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .ifPresent(account -> {
                    throw new InvestmentException(InvestmentErrorCode.ACTIVE_INVESTMENT_ACCOUNT_ALREADY_EXISTS);
                });

        SeasonParticipant participant = new SeasonParticipant(
                userId, "투자자", MVP_DEFAULT_SEASON_ID, MVP_DEFAULT_SEASON_NUMBER);
        InvestmentAccount account = InvestmentAccount.open(participant, DEFAULT_INITIAL_SEED_MONEY);
        InvestmentAccount saved = investmentAccountRepository.save(account);
        return InvestmentAccountResponse.from(saved);
    }

    public InvestmentAccountResponse getMyAccount(UUID userId) {
        InvestmentAccount account = investmentAccountRepository.findByParticipant_InvestorIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));
        return InvestmentAccountResponse.from(account);
    }
}
