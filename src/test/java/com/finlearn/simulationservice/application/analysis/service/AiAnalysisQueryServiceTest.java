package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisDetailResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisListResponse;
import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisForbiddenException;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisNotFoundException;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisQueryServiceTest {

    @Mock
    private AiAnalysisRepository aiAnalysisRepository;

    @InjectMocks
    private AiAnalysisQueryService aiAnalysisQueryService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID OTHER_ACCOUNT_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID AI_ANALYSIS_ID = UUID.randomUUID();

    private AiAnalysis createAnalysis(UUID accountId, LocalDateTime analyzedAt) {
        AiAnalysis analysis = AiAnalysis.create(new CreateAiAnalysisCommand(
                accountId, TARGET_USER_ID, "홍길동",
                SEASON_ID, 1,
                new BigDecimal("65.50"), new BigDecimal("80.00"),
                "ETF 분산 투자 전략", "포트폴리오가 기술주에 편중되어 있습니다.",
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                analyzedAt
        ));
        ReflectionTestUtils.setField(analysis, "aiAnalysisId", AI_ANALYSIS_ID);
        return analysis;
    }

    @Test
    @DisplayName("accountId 기준으로 분석 결과 목록을 최신순으로 반환한다.")
    void getAnalysisList_returnsListOrderedByAnalyzedAtDesc() {
        AiAnalysis older = createAnalysis(ACCOUNT_ID, LocalDateTime.of(2026, 4, 1, 10, 0));
        AiAnalysis newer = createAnalysis(ACCOUNT_ID, LocalDateTime.of(2026, 4, 30, 10, 0));
        when(aiAnalysisRepository.findAllByAccountIdOrderByAnalyzedAtDesc(ACCOUNT_ID))
                .thenReturn(List.of(newer, older));

        List<AiAnalysisListResponse> result = aiAnalysisQueryService.getAnalysisList(ACCOUNT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).analyzedAt()).isAfter(result.get(1).analyzedAt());
    }

    @Test
    @DisplayName("분석 결과가 없는 accountId 조회 시 빈 목록을 반환한다.")
    void getAnalysisList_whenEmpty_returnsEmptyList() {
        when(aiAnalysisRepository.findAllByAccountIdOrderByAnalyzedAtDesc(ACCOUNT_ID))
                .thenReturn(List.of());

        List<AiAnalysisListResponse> result = aiAnalysisQueryService.getAnalysisList(ACCOUNT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("최근 분석 결과를 조회하면 가장 최신 분석을 반환한다.")
    void getLatestAnalysis_returnsLatestAnalysis() {
        AiAnalysis latest = createAnalysis(ACCOUNT_ID, LocalDateTime.of(2026, 4, 30, 10, 0));
        when(aiAnalysisRepository.findTopByAccountIdOrderByAnalyzedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.of(latest));

        AiAnalysisDetailResponse result = aiAnalysisQueryService.getLatestAnalysis(ACCOUNT_ID);

        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.analysisStatus()).isEqualTo(AnalysisStatus.READY);
        assertThat(result.riskScore()).isEqualByComparingTo(new BigDecimal("65.50"));
    }

    @Test
    @DisplayName("최근 분석 결과가 없으면 AiAnalysisNotFoundException을 던진다.")
    void getLatestAnalysis_whenNotFound_throwsAiAnalysisNotFoundException() {
        when(aiAnalysisRepository.findTopByAccountIdOrderByAnalyzedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiAnalysisQueryService.getLatestAnalysis(ACCOUNT_ID))
                .isInstanceOf(AiAnalysisNotFoundException.class);
    }

    @Test
    @DisplayName("분석 결과 단건 상세 조회에 성공하면 모든 필드를 반환한다.")
    void getAnalysisDetail_success_returnsAllFields() {
        AiAnalysis analysis = createAnalysis(ACCOUNT_ID, LocalDateTime.of(2026, 4, 30, 10, 0));
        when(aiAnalysisRepository.findById(AI_ANALYSIS_ID)).thenReturn(Optional.of(analysis));

        AiAnalysisDetailResponse result = aiAnalysisQueryService.getAnalysisDetail(ACCOUNT_ID, AI_ANALYSIS_ID);

        assertThat(result.aiAnalysisId()).isEqualTo(AI_ANALYSIS_ID);
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.targetUserId()).isEqualTo(TARGET_USER_ID);
        assertThat(result.targetUserName()).isEqualTo("홍길동");
        assertThat(result.seasonId()).isEqualTo(SEASON_ID);
        assertThat(result.seasonNumber()).isEqualTo(1);
        assertThat(result.riskScore()).isEqualByComparingTo(new BigDecimal("65.50"));
        assertThat(result.aiFeedbackMessage()).isEqualTo("포트폴리오가 기술주에 편중되어 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 분석 ID 조회 시 AiAnalysisNotFoundException을 던진다.")
    void getAnalysisDetail_whenNotFound_throwsAiAnalysisNotFoundException() {
        when(aiAnalysisRepository.findById(AI_ANALYSIS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiAnalysisQueryService.getAnalysisDetail(ACCOUNT_ID, AI_ANALYSIS_ID))
                .isInstanceOf(AiAnalysisNotFoundException.class);
    }

    @Test
    @DisplayName("다른 계좌의 분석 결과를 조회하면 AiAnalysisForbiddenException을 던진다.")
    void getAnalysisDetail_whenAccountIdMismatch_throwsAiAnalysisForbiddenException() {
        AiAnalysis analysis = createAnalysis(ACCOUNT_ID, LocalDateTime.of(2026, 4, 30, 10, 0));
        when(aiAnalysisRepository.findById(AI_ANALYSIS_ID)).thenReturn(Optional.of(analysis));

        assertThatThrownBy(() -> aiAnalysisQueryService.getAnalysisDetail(OTHER_ACCOUNT_ID, AI_ANALYSIS_ID))
                .isInstanceOf(AiAnalysisForbiddenException.class);
    }
}