package com.finlearn.simulationservice.domain.analysis.entity;

import com.finlearn.common.exception.BadRequestException;
import com.finlearn.simulationservice.domain.analysis.enums.AnalysisStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAnalysisTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();
    private static final String TARGET_USER_NAME = "홍길동";
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final int SEASON_NUMBER = 1;
    private static final BigDecimal RISK_SCORE = new BigDecimal("65.50");
    private static final BigDecimal PORTFOLIO_CONCENTRATION_SCORE = new BigDecimal("80.00");
    private static final String RECOMMENDED_LEARNING_TOPIC = "ETF 분산 투자 전략";
    private static final String AI_FEEDBACK_MESSAGE = "포트폴리오가 기술주에 편중되어 있습니다. 분산 투자를 권장합니다.";
    private static final LocalDateTime PERIOD_START = LocalDateTime.of(2026, 4, 1, 0, 0);
    private static final LocalDateTime PERIOD_END = LocalDateTime.of(2026, 4, 30, 23, 59);
    private static final LocalDateTime ANALYZED_AT = LocalDateTime.of(2026, 4, 30, 10, 0);

    private AiAnalysis createDefault() {
        return AiAnalysis.create(
                ACCOUNT_ID, TARGET_USER_ID, TARGET_USER_NAME,
                SEASON_ID, SEASON_NUMBER,
                RISK_SCORE, PORTFOLIO_CONCENTRATION_SCORE,
                RECOMMENDED_LEARNING_TOPIC, AI_FEEDBACK_MESSAGE,
                PERIOD_START, PERIOD_END, ANALYZED_AT
        );
    }

    @Test
    @DisplayName("유효한 값으로 AI 분석을 생성하면 상태가 READY로 초기화된다.")
    void create_withValidValues_statusIsReady() {
        AiAnalysis aiAnalysis = createDefault();

        assertThat(aiAnalysis.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(aiAnalysis.getTargetUserId()).isEqualTo(TARGET_USER_ID);
        assertThat(aiAnalysis.getTargetUserName()).isEqualTo(TARGET_USER_NAME);
        assertThat(aiAnalysis.getSeasonId()).isEqualTo(SEASON_ID);
        assertThat(aiAnalysis.getSeasonNumber()).isEqualTo(SEASON_NUMBER);
        assertThat(aiAnalysis.getRiskScore()).isEqualByComparingTo(RISK_SCORE);
        assertThat(aiAnalysis.getPortfolioConcentrationScore()).isEqualByComparingTo(PORTFOLIO_CONCENTRATION_SCORE);
        assertThat(aiAnalysis.getRecommendedLearningTopic()).isEqualTo(RECOMMENDED_LEARNING_TOPIC);
        assertThat(aiAnalysis.getAiFeedbackMessage()).isEqualTo(AI_FEEDBACK_MESSAGE);
        assertThat(aiAnalysis.getAnalysisPeriodStartAt()).isEqualTo(PERIOD_START);
        assertThat(aiAnalysis.getAnalysisPeriodEndAt()).isEqualTo(PERIOD_END);
        assertThat(aiAnalysis.getAnalyzedAt()).isEqualTo(ANALYZED_AT);
        assertThat(aiAnalysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.READY);
    }

    @Test
    @DisplayName("accountId가 null이면 생성에 실패한다.")
    void create_withNullAccountId_throwsException() {
        assertThatThrownBy(() -> AiAnalysis.create(
                null, TARGET_USER_ID, TARGET_USER_NAME,
                SEASON_ID, SEASON_NUMBER,
                RISK_SCORE, PORTFOLIO_CONCENTRATION_SCORE,
                RECOMMENDED_LEARNING_TOPIC, AI_FEEDBACK_MESSAGE,
                PERIOD_START, PERIOD_END, ANALYZED_AT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("targetUserId가 null이면 생성에 실패한다.")
    void create_withNullTargetUserId_throwsException() {
        assertThatThrownBy(() -> AiAnalysis.create(
                ACCOUNT_ID, null, TARGET_USER_NAME,
                SEASON_ID, SEASON_NUMBER,
                RISK_SCORE, PORTFOLIO_CONCENTRATION_SCORE,
                RECOMMENDED_LEARNING_TOPIC, AI_FEEDBACK_MESSAGE,
                PERIOD_START, PERIOD_END, ANALYZED_AT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("seasonId가 null이면 생성에 실패한다.")
    void create_withNullSeasonId_throwsException() {
        assertThatThrownBy(() -> AiAnalysis.create(
                ACCOUNT_ID, TARGET_USER_ID, TARGET_USER_NAME,
                null, SEASON_NUMBER,
                RISK_SCORE, PORTFOLIO_CONCENTRATION_SCORE,
                RECOMMENDED_LEARNING_TOPIC, AI_FEEDBACK_MESSAGE,
                PERIOD_START, PERIOD_END, ANALYZED_AT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("recommendedLearningTopic이 blank이면 생성에 실패한다.")
    void create_withBlankRecommendedLearningTopic_throwsException() {
        assertThatThrownBy(() -> AiAnalysis.create(
                ACCOUNT_ID, TARGET_USER_ID, TARGET_USER_NAME,
                SEASON_ID, SEASON_NUMBER,
                RISK_SCORE, PORTFOLIO_CONCENTRATION_SCORE,
                "  ", AI_FEEDBACK_MESSAGE,
                PERIOD_START, PERIOD_END, ANALYZED_AT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("aiFeedbackMessage가 blank이면 생성에 실패한다.")
    void create_withBlankAiFeedbackMessage_throwsException() {
        assertThatThrownBy(() -> AiAnalysis.create(
                ACCOUNT_ID, TARGET_USER_ID, TARGET_USER_NAME,
                SEASON_ID, SEASON_NUMBER,
                RISK_SCORE, PORTFOLIO_CONCENTRATION_SCORE,
                RECOMMENDED_LEARNING_TOPIC, "  ",
                PERIOD_START, PERIOD_END, ANALYZED_AT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("분석 시작 기간이 종료 기간보다 이후이면 생성에 실패한다.")
    void create_withInvalidPeriod_throwsException() {
        assertThatThrownBy(() -> AiAnalysis.create(
                ACCOUNT_ID, TARGET_USER_ID, TARGET_USER_NAME,
                SEASON_ID, SEASON_NUMBER,
                RISK_SCORE, PORTFOLIO_CONCENTRATION_SCORE,
                RECOMMENDED_LEARNING_TOPIC, AI_FEEDBACK_MESSAGE,
                PERIOD_END, PERIOD_START, ANALYZED_AT
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("complete() 호출 시 상태가 COMPLETED로 변경된다.")
    void complete_changesStatusToCompleted() {
        AiAnalysis aiAnalysis = createDefault();

        aiAnalysis.complete();

        assertThat(aiAnalysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    }

    @Test
    @DisplayName("fail() 호출 시 상태가 FAILED로 변경된다.")
    void fail_changesStatusToFailed() {
        AiAnalysis aiAnalysis = createDefault();

        aiAnalysis.fail();

        assertThat(aiAnalysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
    }
}