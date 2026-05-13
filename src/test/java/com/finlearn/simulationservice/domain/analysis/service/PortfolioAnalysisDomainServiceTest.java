package com.finlearn.simulationservice.domain.analysis.service;

import com.finlearn.simulationservice.domain.analysis.vo.ConcentrationLevel;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.RecommendationType;
import com.finlearn.simulationservice.domain.analysis.vo.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioAnalysisDomainServiceTest {

    private final PortfolioAnalysisDomainService service = new PortfolioAnalysisDomainService();

    @Test
    @DisplayName("단일 종목 비중 70% 이상이면 HIGH 집중도, AGGRESSIVE 리스크를 반환한다.")
    void diagnose_highConcentration_returnsAggressiveRisk() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("75.00"), 1, new BigDecimal("10.00"), new BigDecimal("3.00"), new BigDecimal("0.00"));

        assertThat(result.concentrationLevel()).isEqualTo(ConcentrationLevel.HIGH);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.AGGRESSIVE);
    }

    @Test
    @DisplayName("단일 종목 비중 50% 이상 70% 미만이면 MEDIUM 집중도, NORMAL 리스크를 반환한다.")
    void diagnose_mediumConcentration_returnsNormalRisk() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("60.00"), 2, new BigDecimal("5.00"), new BigDecimal("2.00"), new BigDecimal("0.00"));

        assertThat(result.concentrationLevel()).isEqualTo(ConcentrationLevel.MEDIUM);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.NORMAL);
    }

    @Test
    @DisplayName("단일 종목 비중 50% 미만이면 LOW 집중도, STABLE 리스크를 반환한다.")
    void diagnose_lowConcentration_returnsStableRisk() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("30.00"), 4, new BigDecimal("10.00"), new BigDecimal("2.00"), new BigDecimal("20.00"));

        assertThat(result.concentrationLevel()).isEqualTo(ConcentrationLevel.LOW);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.STABLE);
    }

    @Test
    @DisplayName("단일 종목 비중 70% 이상이면 집중도 경고가 포함된다.")
    void diagnose_highConcentration_includesConcentrationWarning() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("80.00"), 2, new BigDecimal("5.00"), new BigDecimal("2.00"), new BigDecimal("0.00"));

        assertThat(result.warnings()).anyMatch(w -> w.contains("단일 종목 비중"));
    }

    @Test
    @DisplayName("보유 종목 수 1~2개이면 분산투자 부족 경고가 포함된다.")
    void diagnose_lowHoldingCount_includesWarning() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("40.00"), 2, new BigDecimal("5.00"), new BigDecimal("2.00"), new BigDecimal("0.00"));

        assertThat(result.warnings()).anyMatch(w -> w.contains("보유 종목 수"));
    }

    @Test
    @DisplayName("현금 비중 50% 이상이면 투자 미진행 경고가 포함된다.")
    void diagnose_highCashWeight_includesWarning() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("0.00"), 0, new BigDecimal("80.00"), new BigDecimal("0.00"), new BigDecimal("0.00"));

        assertThat(result.warnings()).anyMatch(w -> w.contains("현금 비중"));
    }

    @Test
    @DisplayName("수익률 -5% 이하이면 손실 경고가 포함된다.")
    void diagnose_lossReturnRate_includesWarning() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("30.00"), 3, new BigDecimal("10.00"), new BigDecimal("-6.00"), new BigDecimal("10.00"));

        assertThat(result.warnings()).anyMatch(w -> w.contains("손실 구간"));
    }

    @Test
    @DisplayName("HIGH 집중도이면 분산투자 PORTFOLIO 추천이 포함된다.")
    void diagnose_highConcentration_includesPortfolioRecommendation() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("75.00"), 1, new BigDecimal("5.00"), new BigDecimal("2.00"), new BigDecimal("0.00"));

        assertThat(result.recommendations())
                .anyMatch(r -> r.recommendationType() == RecommendationType.PORTFOLIO);
    }

    @Test
    @DisplayName("수익률 +5% 이상이면 고급 전략 QUIZ 추천이 포함된다.")
    void diagnose_highReturnRate_includesAdvancedQuizRecommendation() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("30.00"), 4, new BigDecimal("10.00"), new BigDecimal("8.00"), new BigDecimal("20.00"));

        assertThat(result.recommendations())
                .anyMatch(r -> r.recommendationType() == RecommendationType.QUIZ
                        && r.targetCategory().equals("ADVANCED_STRATEGY"));
    }

    @Test
    @DisplayName("수익률 -5% 이하이면 손실 관리 QUIZ 추천이 포함된다.")
    void diagnose_lossReturnRate_includesRiskManagementRecommendation() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("30.00"), 4, new BigDecimal("10.00"), new BigDecimal("-8.00"), new BigDecimal("20.00"));

        assertThat(result.recommendations())
                .anyMatch(r -> r.recommendationType() == RecommendationType.QUIZ
                        && r.targetCategory().equals("RISK_MANAGEMENT"));
    }

    @Test
    @DisplayName("현금 비중 50% 이상이면 요약 메시지에 현금 비중 관련 내용이 포함된다.")
    void diagnose_highCashWeight_summaryMentionsCash() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("0.00"), 0, new BigDecimal("60.00"), new BigDecimal("0.00"), new BigDecimal("0.00"));

        assertThat(result.analysisSummary()).contains("현금 비중");
    }

    @Test
    @DisplayName("ETF 비중이 0%이고 보유 종목이 있으면 ETF 학습 QUIZ 추천이 포함된다.")
    void diagnose_zeroEtfWeight_withHoldings_includesEtfQuizRecommendation() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("40.00"), 2, new BigDecimal("10.00"), new BigDecimal("2.00"), new BigDecimal("0.00"));

        assertThat(result.recommendations())
                .anyMatch(r -> r.recommendationType() == RecommendationType.QUIZ
                        && r.targetCategory().equals("ETF_BASICS"));
    }

    @Test
    @DisplayName("ETF 비중이 0%이더라도 보유 종목이 없으면 ETF 추천이 포함되지 않는다.")
    void diagnose_zeroEtfWeight_noHoldings_doesNotIncludeEtfRecommendation() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("0.00"), 0, new BigDecimal("100.00"), new BigDecimal("0.00"), new BigDecimal("0.00"));

        assertThat(result.recommendations())
                .noneMatch(r -> r.recommendationType() == RecommendationType.QUIZ
                        && "ETF_BASICS".equals(r.targetCategory()));
    }

    @Test
    @DisplayName("ETF 비중이 0% 초과이면 ETF 학습 추천이 포함되지 않는다.")
    void diagnose_nonZeroEtfWeight_doesNotIncludeEtfRecommendation() {
        PortfolioDiagnosis result = service.diagnose(
                new BigDecimal("30.00"), 3, new BigDecimal("10.00"), new BigDecimal("2.00"), new BigDecimal("25.00"));

        assertThat(result.recommendations())
                .noneMatch(r -> r.recommendationType() == RecommendationType.QUIZ
                        && "ETF_BASICS".equals(r.targetCategory()));
    }
}
