package com.finlearn.simulationservice.domain.analysis.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisDomainException;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisErrorCode;
import com.finlearn.simulationservice.domain.analysis.vo.AnalysisScore;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID aiAnalysisId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private UUID targetUserId;

    @Column(nullable = false, length = 20)
    private String targetUserName;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private int seasonNumber;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "risk_score", nullable = false, precision = 5, scale = 2))
    })
    private AnalysisScore riskScore;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "portfolio_concentration_score", nullable = false, precision = 5, scale = 2))
    })
    private AnalysisScore portfolioConcentrationScore;

    @Column(nullable = false)
    private String recommendedLearningTopic;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiFeedbackMessage;

    @Column(nullable = false)
    private LocalDateTime analysisPeriodStartAt;

    @Column(nullable = false)
    private LocalDateTime analysisPeriodEndAt;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus analysisStatus;

    private AiAnalysis(CreateAiAnalysisCommand command) {
        validate(command);
        this.accountId = command.accountId();
        this.targetUserId = command.targetUserId();
        this.targetUserName = command.targetUserName();
        this.seasonId = command.seasonId();
        this.seasonNumber = command.seasonNumber();
        this.riskScore = AnalysisScore.of(command.riskScore());
        this.portfolioConcentrationScore = AnalysisScore.of(command.portfolioConcentrationScore());
        this.recommendedLearningTopic = command.recommendedLearningTopic();
        this.aiFeedbackMessage = command.aiFeedbackMessage();
        this.analysisPeriodStartAt = command.analysisPeriodStartAt();
        this.analysisPeriodEndAt = command.analysisPeriodEndAt();
        this.analyzedAt = command.analyzedAt();
        this.analysisStatus = AnalysisStatus.READY;
    }

    public static AiAnalysis create(CreateAiAnalysisCommand command) {
        return new AiAnalysis(command);
    }

    public void complete() {
        if (!this.analysisStatus.canComplete()) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.CANNOT_COMPLETE);
        }
        this.analysisStatus = AnalysisStatus.COMPLETED;
    }

    public void fail() {
        if (!this.analysisStatus.canFail()) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.CANNOT_FAIL);
        }
        this.analysisStatus = AnalysisStatus.FAILED;
    }

    private static void validate(CreateAiAnalysisCommand command) {
        if (command.accountId() == null) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_ACCOUNT_ID);
        }
        if (command.targetUserId() == null) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_TARGET_USER_ID);
        }
        if (command.targetUserName() == null || command.targetUserName().isBlank()) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_TARGET_USER_NAME);
        }
        if (command.seasonId() == null) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_SEASON_ID);
        }
        if (command.seasonNumber() <= 0) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_SEASON_NUMBER);
        }
        if (command.riskScore() == null || command.riskScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_RISK_SCORE);
        }
        if (command.portfolioConcentrationScore() == null || command.portfolioConcentrationScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_PORTFOLIO_CONCENTRATION_SCORE);
        }
        if (command.recommendedLearningTopic() == null || command.recommendedLearningTopic().isBlank()) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_RECOMMENDED_LEARNING_TOPIC);
        }
        if (command.aiFeedbackMessage() == null || command.aiFeedbackMessage().isBlank()) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_AI_FEEDBACK_MESSAGE);
        }
        if (command.analysisPeriodStartAt() == null) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_ANALYSIS_PERIOD_START_AT);
        }
        if (command.analysisPeriodEndAt() == null) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_ANALYSIS_PERIOD_END_AT);
        }
        if (!command.analysisPeriodStartAt().isBefore(command.analysisPeriodEndAt())) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_ANALYSIS_PERIOD);
        }
        if (command.analyzedAt() == null) {
            throw new AiAnalysisDomainException(AiAnalysisErrorCode.INVALID_ANALYZED_AT);
        }
    }
}