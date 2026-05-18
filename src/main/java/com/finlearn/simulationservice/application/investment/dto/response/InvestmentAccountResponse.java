package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentAccountResponse(
        UUID investmentAccountId,
        BigDecimal cashBalance,
        BigDecimal initialSeedMoney,
        InvestmentAccountStatus status,
        BigDecimal totalEvaluationAmount,
        BigDecimal totalProfitLoss,
        BigDecimal totalProfitRate
) {

    public static InvestmentAccountResponse from(InvestmentAccount account) {
        BigDecimal initialSeedMoney = BigDecimal.valueOf(account.getInitialSeedMoney());
        BigDecimal cashBalance = BigDecimal.valueOf(account.getCurrentCashBalance());
        BigDecimal totalEvaluationAmount = BigDecimal.valueOf(account.getTotalValuationAmount());
        BigDecimal totalAssetAmount = BigDecimal.valueOf(account.getTotalAssetAmount());
        BigDecimal totalProfitLoss = totalAssetAmount.subtract(initialSeedMoney);
        BigDecimal totalProfitRate = account.getTotalReturnRate();

        return new InvestmentAccountResponse(
                account.getAccountId(),
                cashBalance,
                initialSeedMoney,
                account.getStatus(),
                totalEvaluationAmount,
                totalProfitLoss,
                totalProfitRate
        );
    }
}
