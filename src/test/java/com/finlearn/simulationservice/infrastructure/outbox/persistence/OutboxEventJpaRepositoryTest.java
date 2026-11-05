package com.finlearn.simulationservice.infrastructure.outbox.persistence;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OutboxEventJpaRepositoryTest {

    @Autowired
    private OutboxEventJpaRepository repository;

    @Test
    @DisplayName("발행 가능 시각과 최대 시도 횟수를 만족하는 이벤트만 조회한다.")
    void findPublishableEvents_filtersByRetryPolicy() {
        // given

        // 최초 발행 대상
        OutboxEvent pending = OutboxEvent.of(
                "test.pending", "{}"
        );

        // 첫 번째 발행 실패 후 재시도 가능한 이벤트
        OutboxEvent retryable = OutboxEvent.of(
                "test.retryable", "{}"
        );
        retryable.markProcessing();
        retryable.markFailed();

        // 첫 번째 발행 실패 후 아직 대기 중인 이벤트
        OutboxEvent waiting = OutboxEvent.of(
                "test.waiting", "{}"
        );
        waiting.markProcessing();
        waiting.markFailed();

        // 최대 3회 발행 시도 후 모두 실패한 이벤트
        OutboxEvent exhausted = OutboxEvent.of(
                "test.exhausted", "{}"
        );

        for (int i = 0; i < 3; i++) {
            exhausted.markProcessing();
            exhausted.markFailed();
        }

        repository.saveAllAndFlush(
                List.of(pending, retryable, waiting, exhausted)
        );

        // 상태 및 시도 횟수 사전 검증
        assertThat(pending.getAttemptCount()).isZero();
        assertThat(retryable.getAttemptCount()).isEqualTo(1);
        assertThat(waiting.getAttemptCount()).isEqualTo(1);
        assertThat(exhausted.getAttemptCount()).isEqualTo(3);

        // 6초 후를 기준으로 조회하므로
        // retryable과 waiting 모두 재시도 가능
        LocalDateTime future = LocalDateTime.now()
                .plusSeconds(6);

        // when
        List<OutboxEvent> result =
                repository.findPublishableEventsForUpdate(
                        future,
                        future.minusSeconds(60),
                        3,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result)
                .contains(pending, retryable, waiting)
                .doesNotContain(exhausted);
    }

    @Test
    @DisplayName("재시도 예정 시각이 지나지 않은 FAILED 이벤트는 제외한다.")
    void findPublishableEvents_excludesWaitingEvents() {
        // given
        OutboxEvent pending = OutboxEvent.of(
                "test.pending", "{}"
        );

        OutboxEvent waiting = OutboxEvent.of(
                "test.waiting", "{}"
        );

        waiting.markProcessing();
        waiting.markFailed();

        repository.saveAllAndFlush(
                List.of(pending, waiting)
        );

        // 아직 5초의 대기 시간이 지나지 않은 시점
        LocalDateTime now = LocalDateTime.now();

        // when
        List<OutboxEvent> result =
                repository.findPublishableEventsForUpdate(
                        now,
                        now.minusSeconds(60),
                        3,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result)
                .contains(pending)
                .doesNotContain(waiting);
    }

    @Test
    @DisplayName("FAILED 이벤트는 최대 발행 시도 횟수에 도달하면 조회하지 않는다.")
    void findPublishableEvents_excludesMaxAttemptEvents() {
        // given
        OutboxEvent retryable = OutboxEvent.of(
                "test.retryable", "{}"
        );

        retryable.markProcessing();
        retryable.markFailed();

        OutboxEvent exhausted = OutboxEvent.of(
                "test.exhausted", "{}"
        );

        for (int i = 0; i < 3; i++) {
            exhausted.markProcessing();
            exhausted.markFailed();
        }

        repository.saveAllAndFlush(
                List.of(retryable, exhausted)
        );

        assertThat(retryable.getAttemptCount()).isEqualTo(1);
        assertThat(exhausted.getAttemptCount()).isEqualTo(3);

        // 재시도 대기 시간의 영향을 제거
        LocalDateTime future = LocalDateTime.now()
                .plusMinutes(1);

        // when
        List<OutboxEvent> result =
                repository.findPublishableEventsForUpdate(
                        future,
                        future.minusSeconds(60),
                        3,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result)
                .contains(retryable)
                .doesNotContain(exhausted);
    }

    @Test
    @DisplayName("PROCESSING 상태가 60초 이상 지속되면 재선점 대상으로 조회한다.")
    void findPublishableEvents_includesExpiredProcessing() {
        // given
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        OutboxEvent expired = OutboxEvent.of(
                "test.expired", "{}"
        );

        expired.markProcessing(startedAt);

        repository.saveAndFlush(expired);

        // 처리 시작 후 정확히 60초 경과
        LocalDateTime now = startedAt.plusSeconds(60);

        // when
        List<OutboxEvent> result =
                repository.findPublishableEventsForUpdate(
                        now,
                        now.minusSeconds(60),
                        3,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result).contains(expired);
        assertThat(expired.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("PROCESSING 상태가 60초 미만이면 재선점 대상에서 제외한다.")
    void findPublishableEvents_excludesActiveProcessing() {
        // given
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        OutboxEvent processing = OutboxEvent.of(
                "test.processing", "{}"
        );

        processing.markProcessing(startedAt);

        repository.saveAndFlush(processing);

        // 처리 시작 후 59초 경과
        LocalDateTime now = startedAt.plusSeconds(59);

        // when
        List<OutboxEvent> result =
                repository.findPublishableEventsForUpdate(
                        now,
                        now.minusSeconds(60),
                        3,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result).doesNotContain(processing);
    }

    @Test
    @DisplayName("최대 3회 선점한 PROCESSING 이벤트는 타임아웃 후에도 재발행 대상에서 제외한다.")
    void findPublishableEvents_excludesExhaustedProcessing() {
        // given
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        OutboxEvent exhausted = OutboxEvent.of(
                "test.exhausted.processing", "{}"
        );

        // 이전 두 번의 발행 실패
        exhausted.markProcessing(startedAt.minusMinutes(2));
        exhausted.markFailed();

        exhausted.markProcessing(startedAt.minusMinutes(1));
        exhausted.markFailed();

        // 세 번째 선점 후 결과를 기록하지 못한 상황
        exhausted.markProcessing(startedAt);

        repository.saveAndFlush(exhausted);

        assertThat(exhausted.getAttemptCount()).isEqualTo(3);

        // 세 번째 선점으로부터 60초 경과
        LocalDateTime now = startedAt.plusSeconds(60);

        // when
        List<OutboxEvent> result =
                repository.findPublishableEventsForUpdate(
                        now,
                        now.minusSeconds(60),
                        3,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result).doesNotContain(exhausted);
    }

    @Test
    @DisplayName("최대 시도 횟수에 도달하고 타임아웃된 PROCESSING 이벤트만 조회한다.")
    void findExhaustedProcessingEvents_filtersByTimeoutAndAttempts() {
        // given
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        // 3회 선점, 60초 경과
        OutboxEvent exhausted = OutboxEvent.of(
                "test.exhausted", "{}"
        );
        for (int i = 0; i < 3; i++) {
            exhausted.markProcessing(startedAt);
        }

        // 3회 선점, 아직 타임아웃 전
        OutboxEvent active = OutboxEvent.of(
                "test.active", "{}"
        );
        for (int i = 0; i < 3; i++) {
            active.markProcessing(startedAt.plusSeconds(1));
        }

        // 1회 선점, 60초 경과
        OutboxEvent retryable = OutboxEvent.of(
                "test.retryable", "{}"
        );
        retryable.markProcessing(startedAt);

        repository.saveAllAndFlush(
                List.of(exhausted, active, retryable)
        );

        LocalDateTime expiredBefore = startedAt;

        // when
        List<OutboxEvent> result =
                repository.findExhaustedProcessingEventsForUpdate(
                        expiredBefore,
                        3
                );

        // then
        assertThat(result)
                .containsExactly(exhausted);
    }

    @Test
    @DisplayName("발행 가능한 이벤트가 배치 크기보다 많으면 최대 10건만 조회한다.")
    void findPublishableEvents_limitsBatchSize() {
        // given
        List<OutboxEvent> events = new java.util.ArrayList<>();

        // 발행 가능한 PENDING 이벤트 15건 생성
        for (int i = 0; i < 15; i++) {
            events.add(OutboxEvent.of(
                    "test.batch." + i,
                    "{}"
            ));
        }

        repository.saveAllAndFlush(events);

        LocalDateTime now = LocalDateTime.now();

        int maxAttempts = 3;
        int batchSize = 10;

        // when
        List<OutboxEvent> result =
                repository.findPublishableEventsForUpdate(
                        now,
                        now.minusSeconds(60),
                        maxAttempts,
                        PageRequest.of(0, batchSize)
                );

        // then
        assertThat(result).hasSize(10);

        assertThat(repository.count()).isEqualTo(15);

        assertThat(result)
                .allMatch(event ->
                        event.getStatus() == OutboxEventStatus.PENDING
                );

        assertThat(result)
                .allMatch(event ->
                        event.getAttemptCount() == 0
                );
    }



}
