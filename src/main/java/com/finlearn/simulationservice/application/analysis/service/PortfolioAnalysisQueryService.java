package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAllocationResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.domain.analysis.service.PortfolioAnalysisDomainService;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisQueryService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final HoldingRepository holdingRepository;
    private final StockItemRepository stockItemRepository;
    private final PortfolioAnalysisDomainService portfolioAnalysisDomainService;

    @Cacheable(value = "portfolioAnalysis", key = "#query.investorId")
    public PortfolioAnalysisResponse getPortfolioAnalysis(GetPortfolioAnalysisQuery query) {
        InvestmentAccount account = investmentAccountRepository
                .findByParticipant_InvestorIdAndStatus(query.investorId(), InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        List<Holding> holdings = holdingRepository.findAllWithFilter(account.getAccountId(), null);

        PortfolioAllocationResponse allocation = buildAllocation(account, holdings);

        PortfolioDiagnosis diagnosis = portfolioAnalysisDomainService.diagnose(
                allocation.topHoldingWeight(), holdings.size(),
                allocation.cashWeight(), account.getTotalReturnRate());

        return PortfolioAnalysisResponse.of(account, holdings, allocation, diagnosis);
    }

    @CacheEvict(value = "portfolioAnalysis", key = "#investorId")
    public void evictCache(UUID investorId) {
    }

    private PortfolioAllocationResponse buildAllocation(InvestmentAccount account, List<Holding> holdings) {
        long totalValuationAmount = holdings.stream().mapToLong(Holding::getValuationAmount).sum();
        long totalAssetAmount = account.getTotalAssetAmount();

        BigDecimal topHoldingWeight = holdings.stream()
                .map(h -> totalValuationAmount == 0
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(h.getValuationAmount())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalValuationAmount), 2, RoundingMode.HALF_UP))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        BigDecimal cashWeight = totalAssetAmount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(account.getCurrentCashBalance())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAssetAmount), 2, RoundingMode.HALF_UP);

        BigDecimal stockWeight = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal etfWeight = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (!holdings.isEmpty() && totalValuationAmount > 0) {
            List<String> instrumentCodes = holdings.stream()
                    .map(h -> h.getInstrumentCode().getValue())
                    .toList();

            Map<String, StockAssetType> assetTypeMap = stockItemRepository
                    .findAllByStockCodeIn(instrumentCodes).stream()
                    .collect(Collectors.toMap(StockItem::getStockCode, StockItem::getAssetType));

            long stockValuation = holdings.stream()
                    .filter(h -> assetTypeMap.getOrDefault(
                            h.getInstrumentCode().getValue(), StockAssetType.STOCK) == StockAssetType.STOCK)
                    .mapToLong(Holding::getValuationAmount)
                    .sum();

            long etfValuation = holdings.stream()
                    .filter(h -> assetTypeMap.getOrDefault(
                            h.getInstrumentCode().getValue(), StockAssetType.STOCK) == StockAssetType.ETF)
                    .mapToLong(Holding::getValuationAmount)
                    .sum();

            stockWeight = BigDecimal.valueOf(stockValuation)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalValuationAmount), 2, RoundingMode.HALF_UP);

            etfWeight = BigDecimal.valueOf(etfValuation)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalValuationAmount), 2, RoundingMode.HALF_UP);
        }

        return new PortfolioAllocationResponse(stockWeight, etfWeight, cashWeight, topHoldingWeight, holdings.size());
    }
}
