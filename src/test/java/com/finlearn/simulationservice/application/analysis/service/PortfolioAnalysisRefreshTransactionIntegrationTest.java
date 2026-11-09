package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.domain.analysis.service.PortfolioAnalysisDomainService;
import com.finlearn.simulationservice.domain.analysis.vo.ConcentrationLevel;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import com.finlearn.simulationservice.domain.analysis.vo.RecommendationType;
import com.finlearn.simulationservice.domain.analysis.vo.RiskLevel;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PortfolioAnalysisRefreshTransactionIntegrationTest {

    @Autowired
    private PortfolioAnalysisQueryService portfolioAnalysisQueryService;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @MockitoBean
    private InvestmentAccountRepository investmentAccountRepository;

    @MockitoBean
    private HoldingRepository holdingRepository;

    @MockitoBean
    private PortfolioAnalysisDomainService portfolioAnalysisDomainService;

    @MockitoBean
    private AiAnalysisPersistenceService persistenceService;

    @MockitoBean
    private CacheManager cacheManager;

    @Test
    @DisplayName("refresh에서 조회 트랜잭션은 readOnly이고 AI 호출 시 트랜잭션이 비활성화되며 실패 시 Fallback한다.")
    void refresh_readOnlyQueryAndNonTransactionalAiCall_fallbackOnFailure()
            throws Exception {

        // given: Redis 연결 없이 캐시 AOP가 동작하도록 Mock 설정
        Cache mockCache = mock(Cache.class);

        when(cacheManager.getCache("portfolioAnalysis"))
                .thenReturn(mockCache);

        UUID investorId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();

        InvestmentAccount account = InvestmentAccount.open(
                new SeasonParticipant(
                        investorId,
                        "테스터",
                        seasonId,
                        1
                ),
                10_000_000L
        );

        ReflectionTestUtils.setField(
                account,
                "accountId",
                accountId
        );

        List<PortfolioRecommendation> ruleBasedRecommendations = List.of(
                new PortfolioRecommendation(
                        RecommendationType.PORTFOLIO,
                        null,
                        "룰 기반 이유",
                        "룰 기반 메시지"
                )
        );

        PortfolioDiagnosis diagnosis = new PortfolioDiagnosis(
                ConcentrationLevel.HIGH,
                RiskLevel.AGGRESSIVE,
                "포트폴리오 진단",
                List.of(),
                ruleBasedRecommendations
        );

        // 1. 투자 데이터 조회 시점의 트랜잭션 상태 기록
        AtomicReference<Boolean> readTransactionActive =
                new AtomicReference<>();

        AtomicReference<Boolean> readOnlyTransaction =
                new AtomicReference<>();

        when(investmentAccountRepository
                .findByParticipant_InvestorIdAndStatus(
                        investorId,
                        InvestmentAccountStatus.ACTIVE
                ))
                .thenAnswer(invocation -> {

                    readTransactionActive.set(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    );

                    readOnlyTransaction.set(
                            TransactionSynchronizationManager
                                    .isCurrentTransactionReadOnly()
                    );

                    return Optional.of(account);
                });

        when(holdingRepository.findAllWithFilter(accountId, null))
                .thenReturn(List.of());

        when(portfolioAnalysisDomainService.diagnose(
                any(),
                anyInt(),
                any(),
                any(),
                any()
        )).thenReturn(diagnosis);

        // 2. AI API 호출 시점의 트랜잭션 상태 기록
        ChatClient mockChatClient = mock(ChatClient.class);

        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);

        ChatClient.CallResponseSpec callSpec =
                mock(ChatClient.CallResponseSpec.class);

        when(mockChatClient.prompt())
                .thenReturn(requestSpec);

        when(requestSpec.system(anyString()))
                .thenReturn(requestSpec);

        when(requestSpec.user(anyString()))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenReturn(callSpec);

        AtomicReference<Boolean> aiTransactionActive =
                new AtomicReference<>();

        when(callSpec.content()).thenAnswer(invocation -> {

            aiTransactionActive.set(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            throw new IllegalStateException("AI 응답 실패");
        });

        // 3. 실패 이력 저장 서비스 호출 시점의 트랜잭션 상태 기록
        AtomicReference<Boolean> failureHandlingTransactionActive =
                new AtomicReference<>();

        doAnswer(invocation -> {

            failureHandlingTransactionActive.set(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            return null;
        }).when(persistenceService).saveFailed(
                any(),
                eq("AI 응답 실패")
        );

        // Spring 빈 내부의 ChatClient를 테스트용 Mock으로 교체
        Object target =
                AopTestUtils.getUltimateTargetObject(aiAnalysisService);

        Object originalChatClient =
                ReflectionTestUtils.getField(target, "chatClient");

        try {
            ReflectionTestUtils.setField(
                    target,
                    "chatClient",
                    mockChatClient
            );

            // when: 실제 Spring 프록시를 통해 refresh 호출
            PortfolioAnalysisResponse result =
                    portfolioAnalysisQueryService.refresh(investorId);

            // then 1: 데이터 조회 시 읽기 전용 트랜잭션 활성 확인
            assertThat(readTransactionActive.get())
                    .as("투자 데이터 조회가 실제 실행되어야 합니다.")
                    .isNotNull()
                    .isTrue();

            assertThat(readOnlyTransaction.get())
                    .as("투자 데이터 조회 트랜잭션은 readOnly여야 합니다.")
                    .isTrue();

            // then 2: AI API 호출 시 트랜잭션 비활성 확인
            assertThat(aiTransactionActive.get())
                    .as("AI 호출이 실제 실행되어야 합니다.")
                    .isNotNull()
                    .isFalse();

            // then 3: AI 실패 시 규칙 기반 추천으로 Fallback 확인
            assertThat(result.recommendations())
                    .hasSize(1);

            assertThat(result.recommendations().get(0).reason())
                    .isEqualTo("룰 기반 이유");

            assertThat(result.recommendations().get(0).message())
                    .isEqualTo("룰 기반 메시지");

            // then 4: 실패 이력 저장 서비스 호출 확인
            verify(persistenceService).saveFailed(
                    any(),
                    eq("AI 응답 실패")
            );

            // then 5: 실패 이력 저장 호출 전에
            // refresh의 외부 트랜잭션이 활성화되지 않음을 확인
            assertThat(failureHandlingTransactionActive.get())
                    .as("실패 이력 저장 호출 경로에 외부 트랜잭션이 없어야 합니다.")
                    .isNotNull()
                    .isFalse();

        } finally {
            // 다른 테스트에 영향을 주지 않도록 원래 ChatClient 복원
            ReflectionTestUtils.setField(
                    target,
                    "chatClient",
                    originalChatClient
            );
        }
    }
}
