package com.finlearn.simulationservice.application.analysis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAllocationResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import com.finlearn.simulationservice.domain.analysis.service.PortfolioAnalysisDomainService;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisQueryService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final HoldingRepository holdingRepository;
    private final StockItemRepository stockItemRepository;
    private final PortfolioAnalysisDomainService portfolioAnalysisDomainService;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final AiAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "portfolioAnalysis", key = "#query.investorId")
    public PortfolioAnalysisResponse getPortfolioAnalysis(GetPortfolioAnalysisQuery query) {
        PortfolioAnalysisData data = computePortfolioData(query.investorId());
        List<PortfolioRecommendation> resolvedRecommendations =
                resolveRecommendations(data.account().getAccountId(), data.diagnosis().recommendations());
        return PortfolioAnalysisResponse.of(data.account(), data.holdings(), data.allocation(), data.diagnosis(),
                resolvedRecommendations, data.stockReturnRate(), data.etfReturnRate());
    }

    /**
     * AI 분석을 1순위로 시도하고, 실패 시 규칙 기반 결과로 폴백.
     */
    @CacheEvict(value = "portfolioAnalysis", key = "#investorId")
    @Transactional
    public PortfolioAnalysisResponse refresh(UUID investorId) {
        PortfolioAnalysisData data = computePortfolioData(investorId);
        List<PortfolioRecommendation> recommendations;

        try {
            recommendations = aiAnalysisService.callAndSave(
                    data.account(), data.diagnosis(), data.allocation(), data.diagnosis().recommendations());
        } catch (Exception e) {
            log.warn("[PortfolioAnalysis] AI 분석 실패 - 규칙 기반 결과로 대체: {}", e.getMessage());
            aiAnalysisService.saveFailed(
                    data.account(), data.diagnosis(), data.allocation(), data.diagnosis().recommendations(), e.getMessage());
            recommendations = data.diagnosis().recommendations();
        }

        return PortfolioAnalysisResponse.of(data.account(), data.holdings(), data.allocation(), data.diagnosis(),
                recommendations, data.stockReturnRate(), data.etfReturnRate());
    }

    @CacheEvict(value = "portfolioAnalysis", key = "#investorId")
    public void evictCache(UUID investorId) {
    }

    private PortfolioAnalysisData computePortfolioData(UUID investorId) {
        InvestmentAccount account = investmentAccountRepository
                .findByParticipant_InvestorIdAndStatus(investorId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        List<Holding> holdings = holdingRepository.findAllWithFilter(account.getAccountId(), null);

        Map<String, StockAssetType> assetTypeMap = buildAssetTypeMap(holdings);

        PortfolioAllocationResponse allocation = buildAllocation(account, holdings, assetTypeMap);

        long totalBuyAmount = holdings.stream().mapToLong(Holding::getTotalBuyAmount).sum();
        long totalValuationAmount = holdings.stream().mapToLong(Holding::getValuationAmount).sum();
        long totalProfitLoss = totalValuationAmount - totalBuyAmount;
        BigDecimal holdingsReturnRate = totalBuyAmount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(totalProfitLoss)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalBuyAmount), 2, RoundingMode.HALF_UP);

        PortfolioDiagnosis diagnosis = portfolioAnalysisDomainService.diagnose(
                allocation.topHoldingWeight(), holdings.size(),
                allocation.cashWeight(), holdingsReturnRate, allocation.etfWeight());

        BigDecimal stockReturnRate = computeAssetReturnRate(holdings, assetTypeMap, StockAssetType.STOCK);
        BigDecimal etfReturnRate = computeAssetReturnRate(holdings, assetTypeMap, StockAssetType.ETF);

        return new PortfolioAnalysisData(account, holdings, allocation, diagnosis, stockReturnRate, etfReturnRate);
    }

    private List<PortfolioRecommendation> resolveRecommendations(
            UUID accountId, List<PortfolioRecommendation> ruleBasedRecommendations) {
        return aiAnalysisRepository
                .findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(accountId, AnalysisStatus.COMPLETED)
                .map(a -> {
                    try {
                        JsonNode node = objectMapper.readTree(a.getAiFeedbackMessage());
                        return objectMapper.<List<PortfolioRecommendation>>convertValue(
                                node,
                                new TypeReference<List<PortfolioRecommendation>>() {}
                        );
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .orElse(ruleBasedRecommendations);
    }

    private Map<String, StockAssetType> buildAssetTypeMap(List<Holding> holdings) {
        if (holdings.isEmpty()) {
            return Map.of();
        }
        List<String> instrumentCodes = holdings.stream()
                .map(h -> h.getInstrumentCode().getValue())
                .toList();
        return stockItemRepository.findAllByStockCodeIn(instrumentCodes).stream()
                .collect(Collectors.toMap(StockItem::getStockCode, StockItem::getAssetType));
    }

    private PortfolioAllocationResponse buildAllocation(InvestmentAccount account, List<Holding> holdings,
                                                        Map<String, StockAssetType> assetTypeMap) {
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

    private BigDecimal computeAssetReturnRate(List<Holding> holdings, Map<String, StockAssetType> assetTypeMap,
                                              StockAssetType targetType) {
        List<Holding> filtered = holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), StockAssetType.STOCK) == targetType)
                .toList();

        long totalBuyAmount = filtered.stream().mapToLong(Holding::getTotalBuyAmount).sum();
        if (totalBuyAmount == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long totalUnrealizedProfitLoss = filtered.stream().mapToLong(Holding::getUnrealizedProfitLoss).sum();
        return BigDecimal.valueOf(totalUnrealizedProfitLoss)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBuyAmount), 2, RoundingMode.HALF_UP);
    }

    private record PortfolioAnalysisData(
            InvestmentAccount account,
            List<Holding> holdings,
            PortfolioAllocationResponse allocation,
            PortfolioDiagnosis diagnosis,
            BigDecimal stockReturnRate,
            BigDecimal etfReturnRate
    ) {}
}