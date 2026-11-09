package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAllocationResponse;
import com.finlearn.simulationservice.domain.analysis.vo.*;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AiAnalysisTransactionIntegrationTest {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Test
    @DisplayName("외부 AI API 호출 시 DB 트랜잭션이 활성화되지 않는다.")
    void callAndSave_aiCallRunsWithoutTransaction() throws Exception {

        // given: 실제 외부 API를 호출하지 않도록 ChatClient Mock 구성
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec =
                mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);

        AtomicBoolean transactionActive = new AtomicBoolean();

        // AI 응답을 받는 시점에 트랜잭션 활성 여부 기록
        when(callSpec.content()).thenAnswer(invocation -> {
            transactionActive.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );

            // 실제 DB 저장 없이 호출 시점만 검사
            throw new IllegalStateException("AI 호출 시점 확인 완료");
        });

        // Spring 프록시의 실제 대상 객체에 Mock 주입
        Object target = AopTestUtils.getUltimateTargetObject(aiAnalysisService);
        Object originalChatClient = ReflectionTestUtils.getField(target, "chatClient");

        InvestmentAccount account = InvestmentAccount.open(
                new SeasonParticipant(
                        UUID.randomUUID(),
                        "테스터",
                        UUID.randomUUID(),
                        1
                ),
                10_000_000L
        );

        List<PortfolioRecommendation> recommendations = List.of(
                new PortfolioRecommendation(
                        RecommendationType.PORTFOLIO,
                        null,
                        "집중도 높음",
                        "분산 투자 권장"
                )
        );

        PortfolioDiagnosis diagnosis = new PortfolioDiagnosis(
                ConcentrationLevel.HIGH,
                RiskLevel.AGGRESSIVE,
                "단일 종목 집중도가 높습니다.",
                List.of(),
                recommendations
        );

        PortfolioAllocationResponse allocation =
                new PortfolioAllocationResponse(
                        new BigDecimal("86.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("14.00"),
                        new BigDecimal("86.00"),
                        1
                );

        try {
            ReflectionTestUtils.setField(target, "chatClient", chatClient);

            // when: Spring 트랜잭션 프록시를 통해 실제 서비스 호출
            assertThatThrownBy(() ->
                    aiAnalysisService.callAndSave(
                            account,
                            diagnosis,
                            allocation,
                            recommendations
                    )
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AI 호출 시점 확인 완료");

            // then: 외부 AI 호출 시점에 트랜잭션이 없어야 함
            assertThat(transactionActive.get())
                    .as("외부 AI 호출 중 DB 트랜잭션이 활성화되어서는 안 됩니다.")
                    .isFalse();

        } finally {
            ReflectionTestUtils.setField(target, "chatClient", originalChatClient);
        }
    }
}