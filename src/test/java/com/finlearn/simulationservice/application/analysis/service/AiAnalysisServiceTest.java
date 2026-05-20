package com.finlearn.simulationservice.application.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAllocationResponse;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private AiAnalysisRepository aiAnalysisRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ChatClient chatClient;

    // ChatClient 체인 Mock
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

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);

        aiAnalysisService = new AiAnalysisService(aiAnalysisRepository, null, new ObjectMapper(), cacheManager);
        ReflectionTestUtils.setField(aiAnalysisService, "systemPrompt", "금융 전문가 시스템 프롬프트 (테스트용)");
        ReflectionTestUtils.setField(aiAnalysisService, "userPromptTemplate",
                "집중도: {concentrationLevel}, 리스크: {riskLevel}, 요약: {analysisSummary}, 경고: {warnings}\n{recommendations}");
        ReflectionTestUtils.setField(aiAnalysisService, "chatClient", chatClient);

        account = InvestmentAccount.open(
                new SeasonParticipant(INVESTOR_ID, "테스터", SEASON_ID, 1),
                10_000_000L
        );
        ReflectionTestUtils.setField(account, "accountId", ACCOUNT_ID);

        ruleBasedRecommendations = List.of(
                new PortfolioRecommendation(RecommendationType.PORTFOLIO, null, "집중도 높음", "분산 투자 권장"),
                new PortfolioRecommendation(RecommendationType.QUIZ, "DOMESTIC_ETF", "ETF 비중 낮음", "ETF 학습 권장")
        );

        diagnosis = new PortfolioDiagnosis(
                ConcentrationLevel.HIGH, RiskLevel.AGGRESSIVE,
                "단일 종목 집중도가 높습니다.", List.of("집중도 위험"), ruleBasedRecommendations
        );

        allocation = new PortfolioAllocationResponse(
                new BigDecimal("86.00"), BigDecimal.ZERO,
                new BigDecimal("14.00"), new BigDecimal("86.00"), 1
        );
    }

    @Test
    @DisplayName("AI 응답 성공 시 COMPLETED 상태로 저장되고 캐시가 무효화된다.")
    void callAndSave_success_savesCompletedAnalysisAndEvictsCache() throws Exception {
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

        Cache mockCache = mock(Cache.class);
        when(callSpec.content()).thenReturn(aiResponse);
        when(cacheManager.getCache("portfolioAnalysis")).thenReturn(mockCache);

        List<PortfolioRecommendation> result =
                aiAnalysisService.callAndSave(account, diagnosis, allocation, ruleBasedRecommendations);

        ArgumentCaptor<AiAnalysis> captor = ArgumentCaptor.forClass(AiAnalysis.class);
        verify(aiAnalysisRepository).save(captor.capture());
        verify(mockCache).evict(INVESTOR_ID);

        AiAnalysis saved = captor.getValue();
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(saved.getAiFeedbackMessage()).contains("AI가 재작성한 이유");
        assertThat(saved.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(saved.getFailureReason()).isNull();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).reason()).isEqualTo("AI가 재작성한 이유");
    }

    @Test
    @DisplayName("성공 시 원본 recommendationType, targetCategory를 유지하고 AI의 reason, message만 병합한다.")
    void callAndSave_success_mergesOnlyReasonAndMessage() throws Exception {
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

        Cache mockCache = mock(Cache.class);
        when(callSpec.content()).thenReturn(aiResponse);
        when(cacheManager.getCache("portfolioAnalysis")).thenReturn(mockCache);

        List<PortfolioRecommendation> result =
                aiAnalysisService.callAndSave(account, diagnosis, allocation, ruleBasedRecommendations);

        ArgumentCaptor<AiAnalysis> captor = ArgumentCaptor.forClass(AiAnalysis.class);
        verify(aiAnalysisRepository).save(captor.capture());

        String savedJson = captor.getValue().getAiFeedbackMessage();
        assertThat(savedJson).contains("PORTFOLIO");
        assertThat(savedJson).contains("DOMESTIC_ETF");
        assertThat(savedJson).contains("AI가 재작성한 이유");
        assertThat(savedJson).contains("AI ETF 이유");
        assertThat(savedJson).doesNotContain("FOREIGN_ETF");

        // 원본 타입/카테고리 유지 확인
        assertThat(result.get(0).recommendationType()).isEqualTo(RecommendationType.PORTFOLIO);
        assertThat(result.get(1).targetCategory()).isEqualTo("DOMESTIC_ETF");
    }

    @Test
    @DisplayName("AI 호출 실패 시 예외를 던져 호출부가 폴백 처리할 수 있게 한다.")
    void callAndSave_openAiError_throwsException() {
        when(callSpec.content()).thenThrow(new RuntimeException("API timeout"));

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(account, diagnosis, allocation, ruleBasedRecommendations))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API timeout");
    }

    @Test
    @DisplayName("AI 응답 항목 수가 룰 기반과 다르면 IllegalStateException을 던진다.")
    void callAndSave_countMismatch_throwsIllegalStateException() {
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
                aiAnalysisService.callAndSave(account, diagnosis, allocation, ruleBasedRecommendations))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 응답 항목 수 불일치");
    }

    @Test
    @DisplayName("AI 응답 JSON 파싱 실패 시 예외를 던진다.")
    void callAndSave_invalidJson_throwsException() {
        when(callSpec.content()).thenReturn("invalid json");

        assertThatThrownBy(() ->
                aiAnalysisService.callAndSave(account, diagnosis, allocation, ruleBasedRecommendations))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("saveFailed 호출 시 FAILED 상태로 저장된다.")
    void saveFailed_savesFailedAnalysis() {
        aiAnalysisService.saveFailed(account, diagnosis, allocation, ruleBasedRecommendations, "API timeout");

        ArgumentCaptor<AiAnalysis> captor = ArgumentCaptor.forClass(AiAnalysis.class);
        verify(aiAnalysisRepository).save(captor.capture());

        AiAnalysis saved = captor.getValue();
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(saved.getAiFeedbackMessage()).isEqualTo("N/A");
        assertThat(saved.getFailureReason()).isEqualTo("API timeout");
    }

    @Test
    @DisplayName("성공 시 QUIZ 타입 recommendations의 첫 번째 targetCategory가 추천 학습 주제로 저장된다.")
    void callAndSave_success_setsRecommendedLearningTopicFromQuiz() throws Exception {
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

        Cache mockCache = mock(Cache.class);
        when(callSpec.content()).thenReturn(aiResponse);
        when(cacheManager.getCache("portfolioAnalysis")).thenReturn(mockCache);

        aiAnalysisService.callAndSave(account, diagnosis, allocation, ruleBasedRecommendations);

        ArgumentCaptor<AiAnalysis> captor = ArgumentCaptor.forClass(AiAnalysis.class);
        verify(aiAnalysisRepository).save(captor.capture());

        assertThat(captor.getValue().getRecommendedLearningTopic()).isEqualTo("DOMESTIC_ETF");
    }
}