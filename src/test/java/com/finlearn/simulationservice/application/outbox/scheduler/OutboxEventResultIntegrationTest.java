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
@Import(OutboxEventResultService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxEventResultIntegrationTest {

    @Autowired
    private OutboxEventJpaRepository repository;

    @Autowired
    private OutboxEventResultService resultService;

    @Test
    @DisplayName("재선점 후 이전 토큰으로 발행 성공을 기록할 수 없다.")
    void markPublished_rejectsStaleTokenAfterReclaim() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "test.stale", "{}"
        );

        event.markProcessing(LocalDateTime.now().minusMinutes(2));
        UUID oldToken = event.getProcessingToken();

        // 타임아웃 이후 새로운 작업이 재선점
        event.markProcessing(LocalDateTime.now());
        UUID currentToken = event.getProcessingToken();

        UUID eventId = repository.saveAndFlush(event).getId();

        // when
        boolean updated =
                resultService.markPublished(eventId, oldToken);

        // then
        assertThat(updated).isFalse();

        OutboxEvent found = repository.findById(eventId)
                .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(found.getProcessingToken())
                .isEqualTo(currentToken);

        assertThat(found.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("현재 토큰으로 발행 성공을 기록하면 DB에 PUBLISHED가 저장된다.")
    void markPublished_acceptsCurrentToken() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "test.current", "{}"
        );

        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        UUID eventId = repository.saveAndFlush(event).getId();

        // when
        boolean updated =
                resultService.markPublished(eventId, currentToken);

        // then
        assertThat(updated).isTrue();

        OutboxEvent found = repository.findById(eventId)
                .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(OutboxEventStatus.PUBLISHED);

        assertThat(found.getProcessingToken()).isNull();
        assertThat(found.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("이전 토큰으로 실패 결과를 기록해도 최신 상태가 유지된다.")
    void markFailed_rejectsStaleTokenAfterReclaim() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "test.stale.failed", "{}"
        );

        event.markProcessing(LocalDateTime.now().minusMinutes(2));
        UUID oldToken = event.getProcessingToken();

        event.markProcessing(LocalDateTime.now());
        UUID currentToken = event.getProcessingToken();

        UUID eventId = repository.saveAndFlush(event).getId();

        // when
        boolean updated =
                resultService.markFailed(eventId, oldToken);

        // then
        assertThat(updated).isFalse();

        OutboxEvent found = repository.findById(eventId)
                .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(found.getProcessingToken())
                .isEqualTo(currentToken);

        assertThat(found.getRetryCount()).isZero();
    }
}
