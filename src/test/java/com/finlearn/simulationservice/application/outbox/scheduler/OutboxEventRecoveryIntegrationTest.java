package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.infrastructure.outbox.persistence.OutboxEventJpaRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(OutboxEventRecoveryService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxEventRecoveryIntegrationTest {

    @Autowired
    private OutboxEventJpaRepository repository;

    @Autowired
    private OutboxEventRecoveryService recoveryService;

    @Test
    @DisplayName("복구 서비스 실행 후 FAILED 상태와 토큰 제거가 DB에 반영된다.")
    void recoverExhaustedProcessingEvents_persistsChanges() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "test.recovery", "{}"
        );

        LocalDateTime expiredAt =
                LocalDateTime.now().minusMinutes(2);

        for (int i = 0; i < 3; i++) {
            event.markProcessing(expiredAt);
        }

        UUID oldToken = event.getProcessingToken();

        OutboxEvent saved = repository.saveAndFlush(event);
        UUID eventId = saved.getId();

        // when
        int recoveredCount =
                recoveryService.recoverExhaustedProcessingEvents();

        // then
        assertThat(recoveredCount).isEqualTo(1);

        // 복구 트랜잭션이 종료된 뒤 DB에서 다시 조회
        OutboxEvent recovered = repository.findById(eventId)
                .orElseThrow();

        assertThat(recovered.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(recovered.getAttemptCount())
                .isEqualTo(3);

        assertThat(recovered.getRetryCount())
                .isZero();

        assertThat(recovered.getNextRetryAt()).isNull();
        assertThat(recovered.getProcessingStartedAt()).isNull();
        assertThat(recovered.getProcessingToken()).isNull();

        // 이전 토큰으로는 결과를 변경할 수 없음
        assertThat(recovered.markPublished(oldToken))
                .isFalse();
    }

    @Test
    @DisplayName("최대 시도 횟수에 도달하지 않은 이벤트는 최종 실패 처리하지 않는다.")
    void recoverExhaustedProcessingEvents_preservesRetryableEvent() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "test.retryable", "{}"
        );

        event.markProcessing(
                LocalDateTime.now().minusMinutes(2)
        );

        UUID eventId = repository.saveAndFlush(event).getId();

        // when
        int recoveredCount =
                recoveryService.recoverExhaustedProcessingEvents();

        // then
        assertThat(recoveredCount).isZero();

        OutboxEvent found = repository.findById(eventId)
                .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(found.getAttemptCount())
                .isEqualTo(1);

        assertThat(found.getProcessingToken())
                .isNotNull();
    }
}
