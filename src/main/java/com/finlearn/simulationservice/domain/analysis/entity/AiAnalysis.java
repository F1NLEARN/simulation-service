package com.finlearn.simulationservice.domain.analysis.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.common.exception.BadRequestException;
import com.finlearn.simulationservice.domain.analysis.enums.AnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
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

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal portfolioConcentrationScore;

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

    @Builder
    private AiAnalysis(UUID accountId, UUID targetUserId, String targetUserName,
                       UUID seasonId, int seasonNumber,
                       BigDecimal riskScore, BigDecimal portfolioConcentrationScore,
                       String recommendedLearningTopic, String aiFeedbackMessage,
                       LocalDateTime analysisPeriodStartAt, LocalDateTime analysisPeriodEndAt,
                       LocalDateTime analyzedAt) {
        validate(accountId, targetUserId, targetUserName, seasonId, seasonNumber,
                riskScore, portfolioConcentrationScore,
                recommendedLearningTopic, aiFeedbackMessage,
                analysisPeriodStartAt, analysisPeriodEndAt, analyzedAt);
        this.accountId = accountId;
        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.riskScore = riskScore;
        this.portfolioConcentrationScore = portfolioConcentrationScore;
        this.recommendedLearningTopic = recommendedLearningTopic;
        this.aiFeedbackMessage = aiFeedbackMessage;
        this.analysisPeriodStartAt = analysisPeriodStartAt;
        this.analysisPeriodEndAt = analysisPeriodEndAt;
        this.analyzedAt = analyzedAt;
        this.analysisStatus = AnalysisStatus.READY;
    }

    public static AiAnalysis create(UUID accountId, UUID targetUserId, String targetUserName,
                                    UUID seasonId, int seasonNumber,
                                    BigDecimal riskScore, BigDecimal portfolioConcentrationScore,
                                    String recommendedLearningTopic, String aiFeedbackMessage,
                                    LocalDateTime analysisPeriodStartAt, LocalDateTime analysisPeriodEndAt,
                                    LocalDateTime analyzedAt) {
        return AiAnalysis.builder()
                .accountId(accountId)
                .targetUserId(targetUserId)
                .targetUserName(targetUserName)
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .riskScore(riskScore)
                .portfolioConcentrationScore(portfolioConcentrationScore)
                .recommendedLearningTopic(recommendedLearningTopic)
                .aiFeedbackMessage(aiFeedbackMessage)
                .analysisPeriodStartAt(analysisPeriodStartAt)
                .analysisPeriodEndAt(analysisPeriodEndAt)
                .analyzedAt(analyzedAt)
                .build();
    }

    public void complete() {
        this.analysisStatus = AnalysisStatus.COMPLETED;
    }

    public void fail() {
        this.analysisStatus = AnalysisStatus.FAILED;
    }

    private static void validate(UUID accountId, UUID targetUserId, String targetUserName,
                                  UUID seasonId, int seasonNumber,
                                  BigDecimal riskScore, BigDecimal portfolioConcentrationScore,
                                  String recommendedLearningTopic, String aiFeedbackMessage,
                                  LocalDateTime analysisPeriodStartAt, LocalDateTime analysisPeriodEndAt,
                                  LocalDateTime analyzedAt) {
        if (accountId == null) {
            throw new BadRequestException("accountId", "accountId는 null일 수 없습니다.");
        }
        if (targetUserId == null) {
            throw new BadRequestException("targetUserId", "targetUserId는 null일 수 없습니다.");
        }
        if (targetUserName == null || targetUserName.isBlank()) {
            throw new BadRequestException("targetUserName", "targetUserName은 blank일 수 없습니다.");
        }
        if (seasonId == null) {
            throw new BadRequestException("seasonId", "seasonId는 null일 수 없습니다.");
        }
        if (seasonNumber <= 0) {
            throw new BadRequestException("seasonNumber", "seasonNumber는 0보다 커야 합니다.");
        }
        if (riskScore == null || riskScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("riskScore", "riskScore는 0 이상이어야 합니다.");
        }
        if (portfolioConcentrationScore == null || portfolioConcentrationScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("portfolioConcentrationScore", "portfolioConcentrationScore는 0 이상이어야 합니다.");
        }
        if (recommendedLearningTopic == null || recommendedLearningTopic.isBlank()) {
            throw new BadRequestException("recommendedLearningTopic", "recommendedLearningTopic은 blank일 수 없습니다.");
        }
        if (aiFeedbackMessage == null || aiFeedbackMessage.isBlank()) {
            throw new BadRequestException("aiFeedbackMessage", "aiFeedbackMessage는 blank일 수 없습니다.");
        }
        if (analysisPeriodStartAt == null) {
            throw new BadRequestException("analysisPeriodStartAt", "analysisPeriodStartAt은 null일 수 없습니다.");
        }
        if (analysisPeriodEndAt == null) {
            throw new BadRequestException("analysisPeriodEndAt", "analysisPeriodEndAt은 null일 수 없습니다.");
        }
        if (!analysisPeriodStartAt.isBefore(analysisPeriodEndAt)) {
            throw new BadRequestException("analysisPeriodStartAt", "분석 시작 기간은 종료 기간보다 이전이어야 합니다.");
        }
        if (analyzedAt == null) {
            throw new BadRequestException("analyzedAt", "analyzedAt은 null일 수 없습니다.");
        }
    }
}