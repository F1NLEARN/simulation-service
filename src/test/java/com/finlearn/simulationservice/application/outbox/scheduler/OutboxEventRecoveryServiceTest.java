package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventRecoveryServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventRecoveryService recoveryService;

    @Test
    @DisplayName("최대 시도 횟수에 도달한 만료 이벤트를 FAILED로 정리한다.")
    void recoverExhaustedProcessingEvents_marksFailed() {
        // given
        OutboxEvent event = OutboxEvent.of(
                "trade.executed", "{}"
        );

        LocalDateTime expiredAt =
                LocalDateTime.now().minusMinutes(2);

        for (int i = 0; i < 3; i++) {
            event.markProcessing(expiredAt);
        }

        UUID oldToken = event.getProcessingToken();

        when(outboxEventRepository
                .findExhaustedProcessingEventsForUpdate(
                        any(LocalDateTime.class),
                        eq(3)
                ))
                .thenReturn(List.of(event));

        // when
        int recoveredCount =
                recoveryService.recoverExhaustedProcessingEvents();

        // then
        assertThat(recoveredCount).isEqualTo(1);

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getProcessingToken()).isNull();

        // 이전 토큰으로 상태를 변경할 수 없음
        assertThat(event.markPublished(oldToken)).isFalse();

        verify(outboxEventRepository)
                .findExhaustedProcessingEventsForUpdate(
                        any(LocalDateTime.class),
                        eq(3)
                );
    }

    @Test
    @DisplayName("복구 대상이 없으면 0을 반환한다.")
    void recoverExhaustedProcessingEvents_returnsZero() {
        // given
        when(outboxEventRepository
                .findExhaustedProcessingEventsForUpdate(
                        any(LocalDateTime.class),
                        eq(3)
                ))
                .thenReturn(List.of());

        // when
        int recoveredCount =
                recoveryService.recoverExhaustedProcessingEvents();

        // then
        assertThat(recoveredCount).isZero();
    }
}
