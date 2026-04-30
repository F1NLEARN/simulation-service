package com.finlearn.simulationservice.domain.analysis.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiAnalysisErrorCode {

    INVALID_ACCOUNT_ID("AI_INVALID_ACCOUNT_ID", "accountId", "accountId는 null일 수 없습니다."),
    INVALID_TARGET_USER_ID("AI_INVALID_TARGET_USER_ID", "targetUserId", "targetUserId는 null일 수 없습니다."),
    INVALID_TARGET_USER_NAME("AI_INVALID_TARGET_USER_NAME", "targetUserName", "targetUserName은 blank일 수 없습니다."),
    INVALID_SEASON_ID("AI_INVALID_SEASON_ID", "seasonId", "seasonId는 null일 수 없습니다."),
    INVALID_SEASON_NUMBER("AI_INVALID_SEASON_NUMBER", "seasonNumber", "seasonNumber는 0보다 커야 합니다."),
    INVALID_RISK_SCORE("AI_INVALID_RISK_SCORE", "riskScore", "riskScore는 0 이상이어야 합니다."),
    INVALID_PORTFOLIO_CONCENTRATION_SCORE("AI_INVALID_PORTFOLIO_SCORE", "portfolioConcentrationScore", "portfolioConcentrationScore는 0 이상이어야 합니다."),
    INVALID_RECOMMENDED_LEARNING_TOPIC("AI_INVALID_LEARNING_TOPIC", "recommendedLearningTopic", "recommendedLearningTopic은 blank일 수 없습니다."),
    INVALID_AI_FEEDBACK_MESSAGE("AI_INVALID_FEEDBACK_MSG", "aiFeedbackMessage", "aiFeedbackMessage는 blank일 수 없습니다."),
    INVALID_ANALYSIS_PERIOD_START_AT("AI_INVALID_PERIOD_START", "analysisPeriodStartAt", "analysisPeriodStartAt은 null일 수 없습니다."),
    INVALID_ANALYSIS_PERIOD_END_AT("AI_INVALID_PERIOD_END", "analysisPeriodEndAt", "analysisPeriodEndAt은 null일 수 없습니다."),
    INVALID_ANALYSIS_PERIOD("AI_INVALID_PERIOD", "analysisPeriodStartAt", "분석 시작 기간은 종료 기간보다 이전이어야 합니다."),
    INVALID_ANALYZED_AT("AI_INVALID_ANALYZED_AT", "analyzedAt", "analyzedAt은 null일 수 없습니다."),
    CANNOT_COMPLETE("AI_CANNOT_COMPLETE", "analysisStatus", "READY 상태에서만 완료 처리할 수 있습니다."),
    CANNOT_FAIL("AI_CANNOT_FAIL", "analysisStatus", "READY 상태에서만 실패 처리할 수 있습니다.");

    private final String code;
    private final String field;
    private final String message;
}