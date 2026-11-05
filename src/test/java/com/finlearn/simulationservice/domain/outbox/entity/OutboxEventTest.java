package com.finlearn.simulationservice.domain.outbox.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    @DisplayName("Outbox 이벤트는 최초 생성 시 발행 시도 횟수가 0이다.")
    void createEvent_retryCountIsZero() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(event.getRetryCount())
                .isZero();

        assertThat(event.getNextRetryAt())
                .isNull();
    }

    @Test
    @DisplayName("첫 발행 실패 시 재시도 횟수가 증가하고 다음 재시도 시각이 설정된다.")
    void markFailed_schedulesRetry() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        // when
        event.markFailed();

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(event.getRetryCount())
                .isEqualTo(1);

        assertThat(event.getNextRetryAt())
                .isNotNull();
    }


    @Test
    @DisplayName("두 번째 발행 실패 시 10초 후 재시도하도록 설정한다.")
    void markFailed_secondFailure_schedulesRetryAfter10Seconds() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markFailed();

        // when
        LocalDateTime before = LocalDateTime.now();
        event.markFailed();
        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(event.getRetryCount()).isEqualTo(2);

        assertThat(event.getNextRetryAt())
                .isBetween(before.plusSeconds(10),
                        after.plusSeconds(10));
    }

    @Test
    @DisplayName("세 번째 발행 실패 시 자동 재시도를 중단한다.")
    void markFailed_thirdFailure_stopsRetry() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markFailed();
        event.markFailed();

        // when
        event.markFailed();

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(event.getRetryCount()).isEqualTo(3);

        assertThat(event.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("발행 성공 시 PUBLISHED로 변경하고 재시도 시각을 초기화한다.")
    void markPublished_clearsRetrySchedule() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markFailed();

        // when
        event.markPublished();

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PUBLISHED);

        assertThat(event.getPublishedAt()).isNotNull();

        assertThat(event.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태로 변경하면 처리 시작 시각을 기록한다.")
    void markProcessing_recordsStartedAt() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 2, 10, 0);

        // when
        event.markProcessing(startedAt);

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(event.getProcessingStartedAt())
                .isEqualTo(startedAt);
    }

    @Test
    @DisplayName("PROCESSING 상태가 60초를 초과하면 타임아웃으로 판단한다.")
    void processingTimeout_detectsExpiredProcessing() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 2, 10, 0);

        event.markProcessing(startedAt);

        // when
        boolean beforeTimeout = event.isProcessingExpired(
                startedAt.plusSeconds(59),
                60
        );

        boolean atTimeout = event.isProcessingExpired(
                startedAt.plusSeconds(60),
                60
        );

        boolean afterTimeout = event.isProcessingExpired(
                startedAt.plusSeconds(61),
                60
        );

        // then
        assertThat(beforeTimeout).isFalse();
        assertThat(atTimeout).isTrue();
        assertThat(afterTimeout).isTrue();
    }

    @Test
    @DisplayName("기존 markProcessing 호출도 처리 시작 시각을 기록한다.")
    void markProcessing_withoutArgument_recordsStartedAt() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        // when
        LocalDateTime before = LocalDateTime.now();
        event.markProcessing();
        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(event.getProcessingStartedAt())
                .isBetween(before, after);
    }

    @Test
    @DisplayName("PROCESSING 상태로 전환할 때 새로운 처리 토큰을 발급한다.")
    void markProcessing_generatesToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        // when
        event.markProcessing();

        // then
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(event.getProcessingToken())
                .isNotNull();
    }

    @Test
    @DisplayName("재선점하면 이전 처리 토큰을 새로운 토큰으로 교체한다.")
    void markProcessing_reclaimGeneratesNewToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markProcessing();
        UUID oldToken = event.getProcessingToken();

        // when
        event.markProcessing();

        // then
        assertThat(event.getProcessingToken())
                .isNotNull()
                .isNotEqualTo(oldToken);
    }


    @Test
    @DisplayName("이전 처리 토큰으로는 발행 성공 상태를 반영할 수 없다.")
    void markPublished_rejectsStaleToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markProcessing();
        UUID oldToken = event.getProcessingToken();

        // 타임아웃 이후 새로운 작업이 선점했다고 가정
        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        // when
        boolean updated = event.markPublished(oldToken);

        // then
        assertThat(updated).isFalse();

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(event.getProcessingToken())
                .isEqualTo(currentToken);

        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("현재 처리 토큰으로는 발행 성공 상태를 반영할 수 있다.")
    void markPublished_acceptsCurrentToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        // when
        boolean updated = event.markPublished(currentToken);

        // then
        assertThat(updated).isTrue();

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PUBLISHED);

        assertThat(event.getProcessingToken()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("null 처리 토큰으로는 발행 완료 상태를 반영할 수 없다.")
    void markPublished_rejectsNullToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        // when
        boolean updated = event.markPublished((UUID) null);

        // then
        assertThat(updated).isFalse();

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(event.getProcessingToken())
                .isEqualTo(currentToken);

        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("이전 처리 토큰으로는 발행 실패 상태를 반영할 수 없다.")
    void markFailed_rejectsStaleToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markProcessing();
        UUID oldToken = event.getProcessingToken();

        // 다른 작업이 재선점
        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        // when
        boolean updated = event.markFailed(oldToken);

        // then
        assertThat(updated).isFalse();

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(event.getProcessingToken())
                .isEqualTo(currentToken);

        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("현재 처리 토큰으로 발행 실패 상태를 반영할 수 있다.")
    void markFailed_acceptsCurrentToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        // when
        boolean updated = event.markFailed(currentToken);

        // then
        assertThat(updated).isTrue();

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isNotNull();

        assertThat(event.getProcessingToken()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
    }

    @Test
    @DisplayName("이벤트를 선점할 때마다 발행 시도 횟수가 증가한다.")
    void markProcessing_incrementsAttemptCount() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        assertThat(event.getAttemptCount()).isZero();

        // when
        event.markProcessing();

        // then
        assertThat(event.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이벤트 재선점 시에도 발행 시도 횟수가 증가한다.")
    void markProcessing_reclaimIncrementsAttemptCount() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );

        event.markProcessing();

        // when
        event.markProcessing();

        // then
        assertThat(event.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("최대 시도 횟수에 도달한 PROCESSING 이벤트는 타임아웃 후 최종 FAILED로 변경한다.")
    void markFailedIfProcessingExhausted_marksFailed() {
        // given
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{\"test\":true}"
        );

        // 첫 번째, 두 번째 발행 실패
        event.markProcessing(startedAt.minusMinutes(2));
        event.markFailed();

        event.markProcessing(startedAt.minusMinutes(1));
        event.markFailed();

        // 세 번째 선점 후 서버가 종료된 상황
        event.markProcessing(startedAt);

        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        // when
        boolean result = event.markFailedIfProcessingExhausted(
                startedAt.plusSeconds(60),
                60,
                3
        );

        // then
        assertThat(result).isTrue();

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(event.getAttemptCount()).isEqualTo(3);

        // 타임아웃은 Kafka의 명시적 실패 결과가 아니므로
        // 기존 retryCount를 유지한다.
        assertThat(event.getRetryCount()).isEqualTo(2);

        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getProcessingToken()).isNull();
    }

    @Test
    @DisplayName("최대 시도 횟수에 도달했더라도 타임아웃 전에는 상태를 변경하지 않는다.")
    void markFailedIfProcessingExhausted_ignoresActiveProcessing() {
        // given
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{}"
        );

        for (int i = 0; i < 3; i++) {
            event.markProcessing(startedAt);
        }

        UUID originalToken = event.getProcessingToken();

        // when
        boolean result = event.markFailedIfProcessingExhausted(
                startedAt.plusSeconds(59),
                60,
                3
        );

        // then
        assertThat(result).isFalse();
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getProcessingToken())
                .isEqualTo(originalToken);
    }

    @Test
    @DisplayName("시도 횟수가 남아 있는 PROCESSING 이벤트는 최종 실패 처리하지 않는다.")
    void markFailedIfProcessingExhausted_ignoresRetryableProcessing() {
        // given
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        OutboxEvent event = OutboxEvent.of(
                "trade.executed",
                "{}"
        );

        event.markProcessing(startedAt);

        UUID originalToken = event.getProcessingToken();

        // when
        boolean result = event.markFailedIfProcessingExhausted(
                startedAt.plusSeconds(60),
                60,
                3
        );

        // then
        assertThat(result).isFalse();
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getProcessingToken())
                .isEqualTo(originalToken);
    }



}
