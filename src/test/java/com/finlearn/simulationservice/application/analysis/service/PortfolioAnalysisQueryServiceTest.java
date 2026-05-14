package com.finlearn.simulationservice.application.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisType;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import com.finlearn.simulationservice.domain.analysis.service.PortfolioAnalysisDomainService;
import com.finlearn.simulationservice.domain.analysis.vo.ConcentrationLevel;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import com.finlearn.simulationservice.domain.analysis.vo.RecommendationType;
import com.finlearn.simulationservice.domain.analysis.vo.RiskLevel;
import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioAnalysisQueryServiceTest {

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private PortfolioAnalysisDomainService portfolioAnalysisDomainService;

    @Mock
    private AiAnalysisRepository aiAnalysisRepository;

    @Mock
    private AiAnalysisService aiAnalysisService;

    private PortfolioAnalysisQueryService portfolioAnalysisQueryService;

    private static final PortfolioDiagnosis STUB_DIAGNOSIS = new PortfolioDiagnosis(
            ConcentrationLevel.LOW, RiskLevel.STABLE, "분산투자가 잘 구성되어 있습니다.", List.of(), List.of()
    );

    private static final UUID INVESTOR_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        portfolioAnalysisQueryService = new PortfolioAnalysisQueryService(
                investmentAccountRepository,
                holdingRepository,
                stockItemRepository,
                portfolioAnalysisDomainService,
                aiAnalysisRepository,
                aiAnalysisService,
                new ObjectMapper()
        );
    }

    private InvestmentAccount createActiveAccount() {
        InvestmentAccount account = InvestmentAccount.open(
                new SeasonParticipant(INVESTOR_ID, "테스터", SEASON_ID, 1),
                10_000_000L
        );
        ReflectionTestUtils.setField(account, "accountId", ACCOUNT_ID);
        return account;
    }

    private Holding createHolding(String instrumentCode, String holdingName,
                                  long quantity, long averageBuyPrice, long currentPrice) {
        return Holding.create(new CreateHoldingCommand(
                ACCOUNT_ID, holdingName, SEASON_ID, 1,
                instrumentCode, quantity, averageBuyPrice, currentPrice
        ));
    }

    @Test
    @DisplayName("보유 종목이 없으면 빈 목록과 0 값으로 이루어진 분석 결과를 반환한다.")
    void getPortfolioAnalysis_emptyPortfolio_returnsZeroMetrics() {
        InvestmentAccount account = createActiveAccount();
        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any())).thenReturn(STUB_DIAGNOSIS);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED)).thenReturn(Optional.empty());

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.portfolioSummary().totalBuyAmount()).isZero();
        assertThat(result.portfolioSummary().totalValuationAmount()).isZero();
        assertThat(result.portfolioSummary().totalProfitLoss()).isZero();
        assertThat(result.portfolioSummary().totalReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.holdings()).isEmpty();
        assertThat(result.portfolioSummary().cashBalance()).isEqualTo(10_000_000L);
        assertThat(result.portfolioSummary().totalAssetAmount()).isEqualTo(10_000_000L);
        assertThat(result.allocation().holdingCount()).isZero();
        assertThat(result.allocation().topHoldingWeight()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.allocation().cashWeight()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("단일 보유 종목의 평가 금액, 손익, 수익률을 올바르게 계산한다.")
    void getPortfolioAnalysis_singleHolding_calculatesMetricsCorrectly() {
        InvestmentAccount account = createActiveAccount();
        Holding holding = createHolding("005930", "삼성전자", 10L, 75_000L, 80_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holding));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any())).thenReturn(STUB_DIAGNOSIS);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED)).thenReturn(Optional.empty());

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.portfolioSummary().totalBuyAmount()).isEqualTo(750_000L);
        assertThat(result.portfolioSummary().totalValuationAmount()).isEqualTo(800_000L);
        assertThat(result.portfolioSummary().totalProfitLoss()).isEqualTo(50_000L);
        assertThat(result.portfolioSummary().totalReturnRate()).isEqualByComparingTo(new BigDecimal("6.67"));
        assertThat(result.holdings()).hasSize(1);
        assertThat(result.holdings().get(0).weight()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("복수 보유 종목의 합산 지표를 올바르게 계산한다.")
    void getPortfolioAnalysis_multipleHoldings_sumsTotalsCorrectly() {
        InvestmentAccount account = createActiveAccount();
        Holding holdingA = createHolding("A001", "종목A", 10L, 10_000L, 12_000L);
        Holding holdingB = createHolding("B001", "종목B", 5L, 20_000L, 18_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holdingA, holdingB));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any())).thenReturn(STUB_DIAGNOSIS);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED)).thenReturn(Optional.empty());

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.portfolioSummary().totalBuyAmount()).isEqualTo(200_000L);
        assertThat(result.portfolioSummary().totalValuationAmount()).isEqualTo(210_000L);
        assertThat(result.portfolioSummary().totalProfitLoss()).isEqualTo(10_000L);
        assertThat(result.portfolioSummary().totalReturnRate()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.holdings()).hasSize(2);
    }

    @Test
    @DisplayName("종목별 포트폴리오 비중을 올바르게 계산하고 합계가 100%이다.")
    void getPortfolioAnalysis_multipleHoldings_calculatesWeightsCorrectly() {
        InvestmentAccount account = createActiveAccount();
        Holding holdingA = createHolding("A001", "종목A", 3L, 100_000L, 100_000L);
        Holding holdingB = createHolding("B001", "종목B", 1L, 100_000L, 100_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holdingA, holdingB));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any())).thenReturn(STUB_DIAGNOSIS);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED)).thenReturn(Optional.empty());

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.holdings().get(0).weight()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.holdings().get(1).weight()).isEqualByComparingTo(new BigDecimal("25.00"));
        BigDecimal totalWeight = result.holdings().stream()
                .map(h -> h.weight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalWeight).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.allocation().holdingCount()).isEqualTo(2);
        assertThat(result.allocation().topHoldingWeight()).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    @DisplayName("현재가 하락 시 손실이 분석 결과에 반영된다.")
    void getPortfolioAnalysis_priceDecline_reflectsLoss() {
        InvestmentAccount account = createActiveAccount();
        Holding holding = createHolding("005930", "삼성전자", 10L, 100_000L, 90_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holding));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any())).thenReturn(STUB_DIAGNOSIS);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED)).thenReturn(Optional.empty());

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.portfolioSummary().totalBuyAmount()).isEqualTo(1_000_000L);
        assertThat(result.portfolioSummary().totalValuationAmount()).isEqualTo(900_000L);
        assertThat(result.portfolioSummary().totalProfitLoss()).isEqualTo(-100_000L);
        assertThat(result.portfolioSummary().totalReturnRate()).isEqualByComparingTo(new BigDecimal("-10.00"));
    }

    @Test
    @DisplayName("StockItem 조회 결과로 STOCK/ETF 비중을 올바르게 계산한다.")
    void getPortfolioAnalysis_withStockAndEtf_calculatesAssetTypeWeights() {
        InvestmentAccount account = createActiveAccount();
        Holding holdingA = createHolding("A001", "삼성전자", 3L, 100_000L, 100_000L);
        Holding holdingB = createHolding("B001", "KODEX200", 1L, 100_000L, 100_000L);

        StockItem stockItemA = StockItem.create("삼성전자", "A001", StockAssetType.STOCK);
        StockItem stockItemB = StockItem.create("KODEX200", "B001", StockAssetType.ETF);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holdingA, holdingB));
        when(stockItemRepository.findAllByStockCodeIn(any())).thenReturn(List.of(stockItemA, stockItemB));
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any())).thenReturn(STUB_DIAGNOSIS);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED)).thenReturn(Optional.empty());

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.allocation().stockWeight()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.allocation().etfWeight()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("ACTIVE 투자계좌가 없으면 InvestmentException을 던진다.")
    void getPortfolioAnalysis_noActiveAccount_throwsInvestmentException() {
        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID)))
                .isInstanceOf(InvestmentException.class);
    }

    @Test
    @DisplayName("COMPLETED AI 분석이 있으면 AI recommendations을 반환한다.")
    void getPortfolioAnalysis_completedAiAnalysis_returnsAiRecommendations() throws Exception {
        InvestmentAccount account = createActiveAccount();
        List<PortfolioRecommendation> ruleBasedRecommendations = List.of(
                new PortfolioRecommendation(RecommendationType.PORTFOLIO, null, "룰 기반 이유", "룰 기반 메시지")
        );
        PortfolioDiagnosis diagnosisWithRecommendations = new PortfolioDiagnosis(
                ConcentrationLevel.HIGH, RiskLevel.AGGRESSIVE, "요약", List.of(), ruleBasedRecommendations
        );

        String aiFeedbackJson = """
                [{"recommendationType":"PORTFOLIO","targetCategory":null,"reason":"AI 이유","message":"AI 메시지"}]""";
        AiAnalysis completedAnalysis = createCompletedAnalysis(aiFeedbackJson);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any()))
                .thenReturn(diagnosisWithRecommendations);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED))
                .thenReturn(Optional.of(completedAnalysis));

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).reason()).isEqualTo("AI 이유");
        assertThat(result.recommendations().get(0).message()).isEqualTo("AI 메시지");
    }

    @Test
    @DisplayName("COMPLETED AI 분석이 없으면 룰 기반 recommendations을 반환한다.")
    void getPortfolioAnalysis_noCompletedAiAnalysis_returnsRuleBasedRecommendations() {
        InvestmentAccount account = createActiveAccount();
        List<PortfolioRecommendation> ruleBasedRecommendations = List.of(
                new PortfolioRecommendation(RecommendationType.PORTFOLIO, null, "룰 기반 이유", "룰 기반 메시지")
        );
        PortfolioDiagnosis diagnosisWithRecommendations = new PortfolioDiagnosis(
                ConcentrationLevel.HIGH, RiskLevel.AGGRESSIVE, "요약", List.of(), ruleBasedRecommendations
        );

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any()))
                .thenReturn(diagnosisWithRecommendations);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED))
                .thenReturn(Optional.empty());

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).reason()).isEqualTo("룰 기반 이유");
    }

    @Test
    @DisplayName("AI 분석 aiFeedbackMessage 파싱 실패 시 룰 기반 recommendations로 fallback한다.")
    void getPortfolioAnalysis_aiParsingFailure_fallbackToRuleBased() {
        InvestmentAccount account = createActiveAccount();
        List<PortfolioRecommendation> ruleBasedRecommendations = List.of(
                new PortfolioRecommendation(RecommendationType.PORTFOLIO, null, "룰 기반 이유", "룰 기반 메시지")
        );
        PortfolioDiagnosis diagnosisWithRecommendations = new PortfolioDiagnosis(
                ConcentrationLevel.HIGH, RiskLevel.AGGRESSIVE, "요약", List.of(), ruleBasedRecommendations
        );

        AiAnalysis completedAnalysisWithInvalidJson = createCompletedAnalysis("invalid-json");

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of());
        when(portfolioAnalysisDomainService.diagnose(any(), anyInt(), any(), any(), any()))
                .thenReturn(diagnosisWithRecommendations);
        when(aiAnalysisRepository.findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(ACCOUNT_ID, AnalysisStatus.COMPLETED))
                .thenReturn(Optional.of(completedAnalysisWithInvalidJson));

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).reason()).isEqualTo("룰 기반 이유");
    }

    private AiAnalysis createCompletedAnalysis(String aiFeedbackMessage) {
        LocalDateTime now = LocalDateTime.now();
        CreateAiAnalysisCommand command = new CreateAiAnalysisCommand(
                ACCOUNT_ID, INVESTOR_ID, "테스터",
                SEASON_ID, 1,
                AnalysisType.PORTFOLIO,
                BigDecimal.valueOf(30), BigDecimal.valueOf(50),
                "없음", "요약", aiFeedbackMessage,
                now.toLocalDate().atStartOfDay(), now, now,
                null, null
        );
        AiAnalysis analysis = AiAnalysis.create(command);
        analysis.complete();
        return analysis;
    }

}