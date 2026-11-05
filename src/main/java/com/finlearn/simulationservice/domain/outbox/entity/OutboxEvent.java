package com.finlearn.simulationservice.domain.outbox.entity;

import com.finlearn.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    private static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_token")
    private UUID processingToken;

    public static OutboxEvent of(String topic, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.topic = topic;
        event.payload = payload;
        event.status = OutboxEventStatus.PENDING;
        event.retryCount = 0;
        event.attemptCount = 0;
        event.nextRetryAt = null;
        event.processingToken = null;
        return event;
    }

    public void markProcessing() {
        markProcessing(LocalDateTime.now());
    }

    public void markProcessing(LocalDateTime startedAt) {
        LocalDateTime validStartedAt = java.util.Objects.requireNonNull(
                startedAt,
                "처리 시작 시각은 null일 수 없습니다."
        );

        this.status = OutboxEventStatus.PROCESSING;
        this.processingStartedAt = validStartedAt;
        this.processingToken = UUID.randomUUID();

        this.attemptCount++;
    }

    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.nextRetryAt = null;
        this.processingStartedAt = null;
        this.processingToken = null;
    }

    public boolean markPublished(UUID token) {
        if (status != OutboxEventStatus.PROCESSING
                || token == null
                || !token.equals(processingToken)) {
            return false;
        }

        markPublished();
        return true;
    }

    public void markFailed() {
        this.status = OutboxEventStatus.FAILED;
        this.retryCount++;

        if (retryCount < MAX_ATTEMPTS) {
            long delaySeconds = retryCount == 1 ? 5 : 10;
            this.nextRetryAt =
                    LocalDateTime.now().plusSeconds(delaySeconds);
        } else {
            this.nextRetryAt = null;
        }

        this.processingStartedAt = null;
        this.processingToken = null;
    }

    public boolean markFailed(UUID token) {
        if (status != OutboxEventStatus.PROCESSING
                || token == null
                || !token.equals(processingToken)) {
            return false;
        }

        markFailed();
        return true;
    }

    public boolean isProcessingExpired(LocalDateTime now, long timeoutSeconds) {
        if (status != OutboxEventStatus.PROCESSING
                || processingStartedAt == null) {
            return false;
        }

        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException(
                    "타임아웃은 0초보다 커야 합니다."
            );
        }

        return !processingStartedAt
                .plusSeconds(timeoutSeconds)
                .isAfter(now);
    }

    public boolean markFailedIfProcessingExhausted(
            LocalDateTime now,
            long timeoutSeconds,
            int maxAttempts
    ) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "최대 시도 횟수는 0보다 커야 합니다."
            );
        }

        // 최대 시도 횟수에 도달하지 않았다면 종료하지 않음
        if (attemptCount < maxAttempts) {
            return false;
        }

        // PROCESSING 상태가 아니거나 타임아웃 전이면 종료하지 않음
        if (!isProcessingExpired(now, timeoutSeconds)) {
            return false;
        }

        // 자동 재발행 종료
        this.status = OutboxEventStatus.FAILED;

        // 발행 결과를 확인하지 못했으므로 retryCount는 증가시키지 않음
        this.nextRetryAt = null;

        // 이전 작업의 결과가 뒤늦게 반영되지 않도록 토큰 무효화
        this.processingStartedAt = null;
        this.processingToken = null;

        return true;
    }


}
