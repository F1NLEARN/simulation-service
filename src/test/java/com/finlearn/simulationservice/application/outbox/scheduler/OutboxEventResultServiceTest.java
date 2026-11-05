package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventResultServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @InjectMocks
    private OutboxEventResultService resultService;

    private OutboxEvent event;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );
        eventId = UUID.randomUUID();
    }

    @Test
    @DisplayName("현재 토큰으로 발행 성공 결과를 저장한다.")
    void markPublished_withCurrentToken() {
        // given
        event.markProcessing();
        UUID token = event.getProcessingToken();

        when(repository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        // when
        boolean updated =
                resultService.markPublished(eventId, token);

        // then
        assertThat(updated).isTrue();
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("오래된 토큰으로 발행 성공 결과를 저장하지 않는다.")
    void markPublished_withStaleToken() {
        // given
        event.markProcessing();
        UUID oldToken = event.getProcessingToken();

        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        when(repository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        // when
        boolean updated =
                resultService.markPublished(eventId, oldToken);

        // then
        assertThat(updated).isFalse();
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getProcessingToken())
                .isEqualTo(currentToken);
    }

    @Test
    @DisplayName("현재 토큰으로 발행 실패 결과를 저장한다.")
    void markFailed_withCurrentToken() {
        // given
        event.markProcessing();
        UUID token = event.getProcessingToken();

        when(repository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        // when
        boolean updated =
                resultService.markFailed(eventId, token);

        // then
        assertThat(updated).isTrue();
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("오래된 토큰으로 발행 실패 결과를 저장하지 않는다.")
    void markFailed_withStaleToken() {
        // given
        event.markProcessing();
        UUID oldToken = event.getProcessingToken();

        event.markProcessing();
        UUID currentToken = event.getProcessingToken();

        when(repository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        // when
        boolean updated =
                resultService.markFailed(eventId, oldToken);

        // then
        assertThat(updated).isFalse();
        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getProcessingToken())
                .isEqualTo(currentToken);
        assertThat(event.getRetryCount()).isZero();
    }
}
