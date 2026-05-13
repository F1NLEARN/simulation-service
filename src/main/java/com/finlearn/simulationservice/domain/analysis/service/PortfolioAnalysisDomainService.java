package com.finlearn.simulationservice.domain.analysis.service;

import com.finlearn.simulationservice.domain.analysis.vo.ConcentrationLevel;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import com.finlearn.simulationservice.domain.analysis.vo.RecommendationType;
import com.finlearn.simulationservice.domain.analysis.vo.RiskLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PortfolioAnalysisDomainService {

    private static final BigDecimal HIGH_CONCENTRATION_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal MEDIUM_CONCENTRATION_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal HIGH_CASH_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal LOSS_THRESHOLD = new BigDecimal("-5");
    private static final BigDecimal PROFIT_THRESHOLD = new BigDecimal("5");
    private static final int LOW_HOLDING_COUNT_THRESHOLD = 2;

    public PortfolioDiagnosis diagnose(BigDecimal topHoldingWeight, int holdingCount,
                                       BigDecimal cashWeight, BigDecimal totalReturnRate) {
        ConcentrationLevel concentrationLevel = diagnoseConcentration(topHoldingWeight);
        RiskLevel riskLevel = diagnoseRisk(concentrationLevel);
        List<String> warnings = generateWarnings(topHoldingWeight, holdingCount, cashWeight, totalReturnRate);
        List<PortfolioRecommendation> recommendations = generateRecommendations(concentrationLevel, totalReturnRate);
        String summary = generateSummary(concentrationLevel, cashWeight, totalReturnRate);

        return new PortfolioDiagnosis(concentrationLevel, riskLevel, summary, warnings, recommendations);
    }

    private ConcentrationLevel diagnoseConcentration(BigDecimal topHoldingWeight) {
        if (topHoldingWeight.compareTo(HIGH_CONCENTRATION_THRESHOLD) >= 0) {
            return ConcentrationLevel.HIGH;
        }
        if (topHoldingWeight.compareTo(MEDIUM_CONCENTRATION_THRESHOLD) >= 0) {
            return ConcentrationLevel.MEDIUM;
        }
        return ConcentrationLevel.LOW;
    }

    private RiskLevel diagnoseRisk(ConcentrationLevel concentrationLevel) {
        return switch (concentrationLevel) {
            case HIGH -> RiskLevel.AGGRESSIVE;
            case MEDIUM -> RiskLevel.NORMAL;
            case LOW -> RiskLevel.STABLE;
        };
    }

    private List<String> generateWarnings(BigDecimal topHoldingWeight, int holdingCount,
                                           BigDecimal cashWeight, BigDecimal totalReturnRate) {
        List<String> warnings = new ArrayList<>();

        if (topHoldingWeight.compareTo(HIGH_CONCENTRATION_THRESHOLD) >= 0) {
            warnings.add(String.format("단일 종목 비중이 %s%%로 매우 높습니다.", topHoldingWeight.toPlainString()));
        }
        if (holdingCount > 0 && holdingCount <= LOW_HOLDING_COUNT_THRESHOLD) {
            warnings.add(String.format("보유 종목 수가 %d개로 분산투자가 부족합니다.", holdingCount));
        }
        if (cashWeight.compareTo(HIGH_CASH_THRESHOLD) >= 0) {
            warnings.add(String.format("현금 비중이 %s%%로 높아 투자가 미진행 상태입니다.", cashWeight.toPlainString()));
        }
        if (totalReturnRate.compareTo(LOSS_THRESHOLD) <= 0) {
            warnings.add(String.format("전체 수익률이 %s%%로 손실 구간입니다.", totalReturnRate.toPlainString()));
        }

        return warnings;
    }

    private List<PortfolioRecommendation> generateRecommendations(ConcentrationLevel concentrationLevel,
                                                                    BigDecimal totalReturnRate) {
        List<PortfolioRecommendation> recommendations = new ArrayList<>();

        if (concentrationLevel == ConcentrationLevel.HIGH || concentrationLevel == ConcentrationLevel.MEDIUM) {
            recommendations.add(new PortfolioRecommendation(
                    RecommendationType.PORTFOLIO,
                    null,
                    "단일 종목 집중도가 높습니다.",
                    "단일 종목 비중을 낮추고 ETF 또는 다른 업종으로 분산해보세요."
            ));
        }
        // ETF 비중 0% 추천은 5순위(ETF 자산군 비중 계산) 구현 후 추가 예정

        if (totalReturnRate.compareTo(PROFIT_THRESHOLD) >= 0) {
            recommendations.add(new PortfolioRecommendation(
                    RecommendationType.QUIZ,
                    "ADVANCED_STRATEGY",
                    "수익률이 양호합니다.",
                    "포트폴리오 고급 운용 전략 학습 퀴즈를 추천합니다."
            ));
        } else if (totalReturnRate.compareTo(LOSS_THRESHOLD) <= 0) {
            recommendations.add(new PortfolioRecommendation(
                    RecommendationType.QUIZ,
                    "RISK_MANAGEMENT",
                    "손실이 발생 중입니다.",
                    "손실 관리와 리스크 분산 전략 학습 퀴즈를 추천합니다."
            ));
        }

        return recommendations;
    }

    private String generateSummary(ConcentrationLevel concentrationLevel, BigDecimal cashWeight,
                                    BigDecimal totalReturnRate) {
        if (cashWeight.compareTo(HIGH_CASH_THRESHOLD) >= 0) {
            return "현금 비중이 높아 투자 효율이 낮습니다.";
        }
        boolean isProfit = totalReturnRate.compareTo(PROFIT_THRESHOLD) >= 0;
        boolean isLoss = totalReturnRate.compareTo(LOSS_THRESHOLD) <= 0;

        return switch (concentrationLevel) {
            case HIGH -> isProfit
                    ? "수익률은 양호하지만 단일 종목 집중도가 높습니다."
                    : isLoss
                    ? "단일 종목 집중도가 높고 손실이 발생 중입니다."
                    : "단일 종목 집중도가 높아 리스크 관리가 필요합니다.";
            case MEDIUM -> isProfit
                    ? "일부 종목에 편중이 있으나 수익률은 양호합니다."
                    : "일부 종목에 편중이 있습니다.";
            case LOW -> isProfit
                    ? "분산투자가 잘 되어 있으며 수익률도 양호합니다."
                    : isLoss
                    ? "분산투자는 양호하지만 손실이 발생 중입니다."
                    : "분산투자가 잘 구성되어 있습니다.";
        };
    }
}
