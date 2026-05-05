package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioAnalysisQueryServiceTest {

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @InjectMocks
    private PortfolioAnalysisQueryService portfolioAnalysisQueryService;

    private static final UUID INVESTOR_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();

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

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.totalBuyAmount()).isZero();
        assertThat(result.totalValuationAmount()).isZero();
        assertThat(result.totalProfitLoss()).isZero();
        assertThat(result.totalReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.holdings()).isEmpty();
    }

    @Test
    @DisplayName("단일 보유 종목의 평가 금액, 손익, 수익률을 올바르게 계산한다.")
    void getPortfolioAnalysis_singleHolding_calculatesMetricsCorrectly() {
        // 10주 @ 매수 75,000원, 현재 80,000원
        // totalBuyAmount = 750,000, valuationAmount = 800,000, profitLoss = 50,000
        // returnRate = 50,000 / 750,000 * 100 ≈ 6.67%
        InvestmentAccount account = createActiveAccount();
        Holding holding = createHolding("005930", "삼성전자", 10L, 75_000L, 80_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holding));

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.totalBuyAmount()).isEqualTo(750_000L);
        assertThat(result.totalValuationAmount()).isEqualTo(800_000L);
        assertThat(result.totalProfitLoss()).isEqualTo(50_000L);
        assertThat(result.totalReturnRate()).isEqualByComparingTo(new BigDecimal("6.67"));
        assertThat(result.holdings()).hasSize(1);
        assertThat(result.holdings().get(0).weight()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("복수 보유 종목의 합산 지표를 올바르게 계산한다.")
    void getPortfolioAnalysis_multipleHoldings_sumsTotalsCorrectly() {
        // 종목A: 10주 @ 10,000원, 현재 12,000원 → buy=100,000, val=120,000
        // 종목B: 5주 @ 20,000원, 현재 18,000원 → buy=100,000, val=90,000
        // total buy=200,000, total val=210,000, profit=10,000, rate=5%
        InvestmentAccount account = createActiveAccount();
        Holding holdingA = createHolding("A001", "종목A", 10L, 10_000L, 12_000L);
        Holding holdingB = createHolding("B001", "종목B", 5L, 20_000L, 18_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holdingA, holdingB));

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.totalBuyAmount()).isEqualTo(200_000L);
        assertThat(result.totalValuationAmount()).isEqualTo(210_000L);
        assertThat(result.totalProfitLoss()).isEqualTo(10_000L);
        assertThat(result.totalReturnRate()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.holdings()).hasSize(2);
    }

    @Test
    @DisplayName("종목별 포트폴리오 비중을 올바르게 계산하고 합계가 100%이다.")
    void getPortfolioAnalysis_multipleHoldings_calculatesWeightsCorrectly() {
        // 종목A: val=300,000 → 비중 75%
        // 종목B: val=100,000 → 비중 25%
        InvestmentAccount account = createActiveAccount();
        Holding holdingA = createHolding("A001", "종목A", 3L, 100_000L, 100_000L);
        Holding holdingB = createHolding("B001", "종목B", 1L, 100_000L, 100_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holdingA, holdingB));

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.holdings().get(0).weight()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.holdings().get(1).weight()).isEqualByComparingTo(new BigDecimal("25.00"));
        BigDecimal totalWeight = result.holdings().stream()
                .map(h -> h.weight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalWeight).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("현재가 하락 시 손실이 분석 결과에 반영된다.")
    void getPortfolioAnalysis_priceDecline_reflectsLoss() {
        // 10주 @ 100,000원 매수, 현재 90,000원 → 손실 -100,000, 수익률 -10%
        InvestmentAccount account = createActiveAccount();
        Holding holding = createHolding("005930", "삼성전자", 10L, 100_000L, 90_000L);

        when(investmentAccountRepository.findByParticipant_InvestorIdAndStatus(INVESTOR_ID, InvestmentAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holding));

        PortfolioAnalysisResponse result = portfolioAnalysisQueryService.getPortfolioAnalysis(
                new GetPortfolioAnalysisQuery(INVESTOR_ID));

        assertThat(result.totalBuyAmount()).isEqualTo(1_000_000L);
        assertThat(result.totalValuationAmount()).isEqualTo(900_000L);
        assertThat(result.totalProfitLoss()).isEqualTo(-100_000L);
        assertThat(result.totalReturnRate()).isEqualByComparingTo(new BigDecimal("-10.00"));
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
}
