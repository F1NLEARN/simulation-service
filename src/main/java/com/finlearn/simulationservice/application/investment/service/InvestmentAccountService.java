package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.response.InvestmentAccountResponse;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentAccountService {

    private static final BigDecimal DEFAULT_INITIAL_SEED_MONEY = new BigDecimal("10000000");

    private final InvestmentAccountRepository investmentAccountRepository;

    @Transactional
    public InvestmentAccountResponse createAccount(String userId) {
        investmentAccountRepository.findByUserIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .ifPresent(account -> {
                    throw new InvestmentException(InvestmentErrorCode.ACTIVE_INVESTMENT_ACCOUNT_ALREADY_EXISTS);
                });

        InvestmentAccount account = InvestmentAccount.openForUser(userId, DEFAULT_INITIAL_SEED_MONEY);
        InvestmentAccount saved = investmentAccountRepository.save(account);
        return InvestmentAccountResponse.from(saved);
    }

    public InvestmentAccountResponse getMyAccount(String userId) {
        InvestmentAccount account = investmentAccountRepository.findByUserIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));
        return InvestmentAccountResponse.from(account);
    }
}
