package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventClaimServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventClaimService claimService;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = OutboxEvent.of(
                "trade.executed",
                "{\"test\": true}"
        );
    }

    @Test
    @DisplayName("발행 가능한 이벤트를 PROCESSING 상태로 선점한다.")
    void claimPublishableEvents_marksProcessing() {
        // given
        when(outboxEventRepository.findPublishableEventsForUpdate(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(3),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        // when
        List<ClaimedOutboxEvent> claimed =
                claimService.claimPublishableEvents();

        // then
        assertThat(claimed).hasSize(1);

        assertThat(event.getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);

        assertThat(event.getProcessingStartedAt())
                .isNotNull();

        assertThat(event.getProcessingToken())
                .isNotNull();

        assertThat(event.getAttemptCount())
                .isEqualTo(1);

        assertThat(claimed.get(0).processingToken())
                .isEqualTo(event.getProcessingToken());

        assertThat(claimed.get(0).topic())
                .isEqualTo("trade.executed");

        assertThat(claimed.get(0).payload())
                .isEqualTo("{\"test\": true}");

        verify(outboxEventRepository)
                .findPublishableEventsForUpdate(
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(3),
                        eq(PageRequest.of(0, 3))
                );
    }

    @Test
    @DisplayName("발행 가능한 이벤트가 없으면 빈 목록을 반환한다.")
    void claimPublishableEvents_returnsEmptyList() {
        // given
        when(outboxEventRepository.findPublishableEventsForUpdate(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(3),
                any(Pageable.class)
        )).thenReturn(List.of());

        // when
        List<ClaimedOutboxEvent> claimed =
                claimService.claimPublishableEvents();

        // then
        assertThat(claimed).isEmpty();
    }

    @Test
    @DisplayName("PROCESSING 타임아웃 기준을 현재 시각보다 120초 전으로 설정한다.")
    void claimPublishableEvents_uses120SecondTimeout() {
        // given
        when(outboxEventRepository.findPublishableEventsForUpdate(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(3),
                any(Pageable.class)
        )).thenReturn(List.of());

        ArgumentCaptor<LocalDateTime> nowCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        ArgumentCaptor<LocalDateTime> expiredCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        // when
        claimService.claimPublishableEvents();

        // then
        verify(outboxEventRepository)
                .findPublishableEventsForUpdate(
                        nowCaptor.capture(),
                        expiredCaptor.capture(),
                        eq(3),
                        any(Pageable.class)
                );

        LocalDateTime now = nowCaptor.getValue();
        LocalDateTime expiredBefore = expiredCaptor.getValue();

        assertThat(expiredBefore)
                .isEqualTo(now.minusSeconds(120));
    }

}
