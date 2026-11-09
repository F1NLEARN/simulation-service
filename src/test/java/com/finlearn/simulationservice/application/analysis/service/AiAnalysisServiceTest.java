package com.finlearn.simulationservice.application.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAllocationResponse;
import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.vo.ConcentrationLevel;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import com.finlearn.simulationservice.domain.analysis.vo.RecommendationType;
import com.finlearn.simulationservice.domain.analysis.vo.RiskLevel;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private AiAnalysisPersistenceService persistenceService;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    private AiAnalysisService aiAnalysisService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID INVESTOR_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();

    private InvestmentAccount account;
    private PortfolioDiagnosis diagnosis;
    private PortfolioAllocationResponse allocation;
    private List<PortfolioRecommendation> ruleBasedRecommendations;

    @BeforeEach
    void setUp() {
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);

        aiAnalysisService = new AiAnalysisService(
                null,
                new ObjectMapper(),
                persistenceService
        );

        ReflectionTestUtils.setField(
                aiAnalysisService,
                "systemPrompt",
                "금융 전문가 시스템 프롬프트 (테스트용)"
        );

        ReflectionTestUtils.setField(
                aiAnalysisService,
                "userPromptTemplate",
                "집중도: {concentrationLevel}, 리스크: {riskLevel}, 요약: {analysisSummary}, 경고: {warnings}\n{recommendations}"
        );

        ReflectionTestUtils.setField(
                aiAnalysisService,
                "chatClient",
                chatClient
        );

        account = InvestmentAccount.open(
                new SeasonParticipant(
                        INVESTOR_ID,
                        "테스터",
                        SEASON_ID,
                        1
                ),
                10_000_000L
        );

        ReflectionTestUtils.setField(
                account,
                "accountId",
                ACCOUNT_ID
        );

        ruleBasedRecommendations = List.of(
                new PortfolioRecommendation(
                        RecommendationType.PORTFOLIO,
                        null,
                        "집중도 높음",
                        "분산 투자 권장"
                ),
                new PortfolioRecommendation(
                        RecommendationType.QUIZ,
                        "DOMESTIC_ETF",
                        "ETF 비중 낮음",
                        "ETF 학습 권장"
                )
        );

        diagnosis = new PortfolioDiagnosis(
                ConcentrationLevel.HIGH,
                RiskLevel.AGGRESSIVE,
                "단일 종목 집중도가 높습니다.",
                List.of("집중도 위험"),
                ruleBasedRecommendations
        );

        allocation = new PortfolioAllocationResponse(
                new BigDecimal("86.00"),
                BigDecimal.ZERO,
                new BigDecimal("14.00"),
                new BigDecimal("86.00"),
                1
        );
    }

    // 실제 외부 AI 호출 없이 ChatClient 호출 흐름을 Mock 처리
    private void mockChatClientChain() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    @DisplayName("AI 응답 성공 시 분석 이력 저장을 위임한다.")
    void callAndSave_success_savesCompletedAnalysis()
            throws Exception {

        mockChatClientChain();

        String aiResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "PORTFOLIO",
                      "targetCategory": null,
                      "reason": "AI가 재작성한 이유",
                      "message": "AI가 재작성한 메시지"
                    },
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": "DOMESTIC_ETF",
                      "reason": "AI ETF 이유",
                      "message": "AI ETF 메시지"
                    }
                  ]
                }""";

        when(callSpec.content()).thenReturn(aiResponse);

        List<PortfolioRecommendation> result =
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                );

        ArgumentCaptor<CreateAiAnalysisCommand> captor =
                ArgumentCaptor.forClass(CreateAiAnalysisCommand.class);

        verify(persistenceService).saveCompleted(captor.capture());

        CreateAiAnalysisCommand command = captor.getValue();

        assertThat(command.accountId())
                .isEqualTo(ACCOUNT_ID);

        assertThat(command.aiFeedbackMessage())
                .contains("AI가 재작성한 이유")
                .contains("AI ETF 메시지");

        assertThat(result).hasSize(2);

        assertThat(result.get(0).reason())
                .isEqualTo("AI가 재작성한 이유");

        assertThat(result.get(0).recommendationType())
                .isEqualTo(RecommendationType.PORTFOLIO);

        assertThat(result.get(1).targetCategory())
                .isEqualTo("DOMESTIC_ETF");
    }

    @Test
    @DisplayName("AI 응답의 추천 유형과 대상이 규칙 기반 결과와 다르면 예외를 발생시킨다.")
    void callAndSave_mismatchedRecommendation_throwsException() {

        mockChatClientChain();

        String aiResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": "FOREIGN_ETF",
                      "reason": "AI가 재작성한 이유",
                      "message": "AI가 재작성한 메시지"
                    },
                    {
                      "recommendationType": "PORTFOLIO",
                      "targetCategory": null,
                      "reason": "AI ETF 이유",
                      "message": "AI ETF 메시지"
                    }
                  ]
                }""";

        when(callSpec.content()).thenReturn(aiResponse);

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 응답 추천 항목 불일치");

        verify(persistenceService, never())
                .saveCompleted(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("AI 호출 실패 시 예외를 던져 호출부가 Fallback 처리할 수 있게 한다.")
    void callAndSave_openAiError_throwsException() {

        mockChatClientChain();

        when(callSpec.content())
                .thenThrow(new RuntimeException("API timeout"));

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API timeout");

        verify(persistenceService, never())
                .saveCompleted(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("AI 응답 항목 수가 규칙 기반 추천과 다르면 예외를 발생시킨다.")
    void callAndSave_countMismatch_throwsIllegalStateException() {

        mockChatClientChain();

        String mismatchedResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "PORTFOLIO",
                      "targetCategory": null,
                      "reason": "이유",
                      "message": "메시지"
                    }
                  ]
                }""";

        when(callSpec.content()).thenReturn(mismatchedResponse);

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 응답 항목 수 불일치");
    }

    @Test
    @DisplayName("AI 응답 JSON 파싱 실패 시 예외를 발생시킨다.")
    void callAndSave_invalidJson_throwsException() {

        mockChatClientChain();

        when(callSpec.content())
                .thenReturn("invalid json");

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("AI 분석 실패 시 실패 이력 저장에 필요한 데이터를 저장 서비스에 전달한다.")
    void saveFailed_delegatesToPersistenceService() {

        aiAnalysisService.saveFailed(
                account,
                diagnosis,
                allocation,
                ruleBasedRecommendations,
                "API timeout"
        );

        ArgumentCaptor<CreateAiAnalysisCommand> captor =
                ArgumentCaptor.forClass(CreateAiAnalysisCommand.class);

        verify(persistenceService).saveFailed(
                captor.capture(),
                eq("API timeout")
        );

        CreateAiAnalysisCommand command = captor.getValue();

        assertThat(command.accountId())
                .isEqualTo(ACCOUNT_ID);

        assertThat(command.aiFeedbackMessage())
                .isEqualTo("N/A");
    }

    @Test
    @DisplayName("정상 AI 분석 시 QUIZ 추천 대상이 추천 학습 주제로 전달된다.")
    void callAndSave_success_setsRecommendedLearningTopicFromQuiz()
            throws Exception {

        mockChatClientChain();

        String aiResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "PORTFOLIO",
                      "targetCategory": null,
                      "reason": "이유1",
                      "message": "메시지1"
                    },
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": "DOMESTIC_ETF",
                      "reason": "이유2",
                      "message": "메시지2"
                    }
                  ]
                }""";


        when(callSpec.content()).thenReturn(aiResponse);

        aiAnalysisService.callAndSave(
                account,
                diagnosis,
                allocation,
                ruleBasedRecommendations
        );

        ArgumentCaptor<CreateAiAnalysisCommand> captor =
                ArgumentCaptor.forClass(CreateAiAnalysisCommand.class);

        verify(persistenceService).saveCompleted(captor.capture());

        assertThat(captor.getValue().recommendedLearningTopic())
                .isEqualTo("DOMESTIC_ETF");
    }

    @Test
    @DisplayName("AI 추천 유형만 다르면 예외를 발생시킨다.")
    void callAndSave_typeMismatch_throwsException() {

        mockChatClientChain();

        String aiResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": null,
                      "reason": "잘못된 추천 유형",
                      "message": "메시지1"
                    },
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": "DOMESTIC_ETF",
                      "reason": "정상 추천",
                      "message": "메시지2"
                    }
                  ]
                }""";

        when(callSpec.content()).thenReturn(aiResponse);

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 응답 추천 항목 불일치");
    }

    @Test
    @DisplayName("AI 추천 대상 카테고리만 다르면 예외를 발생시킨다.")
    void callAndSave_categoryMismatch_throwsException() {

        mockChatClientChain();

        String aiResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "PORTFOLIO",
                      "targetCategory": null,
                      "reason": "정상 추천",
                      "message": "메시지1"
                    },
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": "FOREIGN_ETF",
                      "reason": "잘못된 추천 대상",
                      "message": "메시지2"
                    }
                  ]
                }""";

        when(callSpec.content()).thenReturn(aiResponse);

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 응답 추천 항목 불일치");
    }

    @Test
    @DisplayName("AI 추천 이유가 비어 있으면 예외를 발생시킨다.")
    void callAndSave_blankReason_throwsException() {

        mockChatClientChain();

        String aiResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "PORTFOLIO",
                      "targetCategory": null,
                      "reason": "   ",
                      "message": "분산 투자를 권장합니다."
                    },
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": "DOMESTIC_ETF",
                      "reason": "ETF 비중이 낮습니다.",
                      "message": "ETF 학습을 권장합니다."
                    }
                  ]
                }""";

        when(callSpec.content()).thenReturn(aiResponse);

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 응답 추천 문구 누락");
    }

    @Test
    @DisplayName("AI 추천 메시지가 null이면 예외를 발생시킨다.")
    void callAndSave_nullMessage_throwsException() {

        mockChatClientChain();

        String aiResponse = """
                {
                  "recommendations": [
                    {
                      "recommendationType": "PORTFOLIO",
                      "targetCategory": null,
                      "reason": "집중도가 높습니다.",
                      "message": null
                    },
                    {
                      "recommendationType": "QUIZ",
                      "targetCategory": "DOMESTIC_ETF",
                      "reason": "ETF 비중이 낮습니다.",
                      "message": "ETF 학습을 권장합니다."
                    }
                  ]
                }""";

        when(callSpec.content()).thenReturn(aiResponse);

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(
                        account,
                        diagnosis,
                        allocation,
                        ruleBasedRecommendations
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 응답 추천 문구 누락");
    }
}
