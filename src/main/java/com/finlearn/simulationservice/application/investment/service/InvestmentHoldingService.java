package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.response.HoldingResponse;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentHoldingService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final HoldingRepository holdingRepository;
    private final StockItemRepository stockItemRepository;

    public List<HoldingResponse> getMyHoldings(String userId) {
        InvestmentAccount account = investmentAccountRepository.findByUserIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        List<Holding> holdings = holdingRepository.findAllByAccountId(account.getInvestmentAccountId());

        return holdings.stream()
                .map(this::toHoldingResponse)
                .toList();
    }

    private HoldingResponse toHoldingResponse(Holding holding) {
        StockItem stockItem = stockItemRepository.findByStockCode(holding.getInstrumentCode())
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND));

        BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
        BigDecimal averagePrice = BigDecimal.valueOf(holding.getAverageBuyPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentPrice = stockItem.getCurrentPrice() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : stockItem.getCurrentPrice().setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPurchaseAmount = averagePrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentEvaluationAmount = currentPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitLoss = currentEvaluationAmount.subtract(totalPurchaseAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal profitRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalPurchaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = profitLoss
                    .divide(totalPurchaseAmount, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new HoldingResponse(
                holding.getHoldingId(),
                holding.getInstrumentCode(),
                stockItem.getStockName(),
                holding.getQuantity(),
                averagePrice,
                currentPrice,
                totalPurchaseAmount,
                currentEvaluationAmount,
                profitLoss,
                profitRate
        );
    }
}
