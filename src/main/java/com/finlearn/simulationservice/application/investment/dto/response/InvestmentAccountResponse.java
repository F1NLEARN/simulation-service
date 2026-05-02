package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        BigDecimal initialSeedMoney = account.getSeedMoney();
        BigDecimal totalEvaluationAmount = account.getCashBalance();
        BigDecimal totalProfitLoss = totalEvaluationAmount.subtract(initialSeedMoney);

        BigDecimal totalProfitRate = BigDecimal.ZERO;
        if (initialSeedMoney.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitRate = totalProfitLoss
                    .divide(initialSeedMoney, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new InvestmentAccountResponse(
                account.getInvestmentAccountId(),
                account.getCashBalance(),
                initialSeedMoney,
                account.getStatus(),
                totalEvaluationAmount,
                totalProfitLoss,
                totalProfitRate
        );
    }
}
