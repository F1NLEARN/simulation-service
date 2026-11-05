package com.finlearn.simulationservice.application.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.producer.SimulationKafkaProducer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventSchedulerTest {

    @Mock
    private OutboxEventClaimService claimService;

    @Mock
    private OutboxEventResultService resultService;

    @Mock
    private OutboxEventRecoveryService recoveryService;

    @Mock
    private SimulationKafkaProducer kafkaProducer;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventScheduler scheduler;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 5, 18, 10, 0);

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    private ClaimedOutboxEvent tradeEvent() throws Exception {
        TradeExecutedEvent dto = new TradeExecutedEvent(
                USER_ID,
                ACCOUNT_ID,
                SEASON_ID,
                1,
                "BUY",
                "STOCK",
                "005930",
                1,
                5.0,
                6.0,
                7.0,
                0.0,
                "testUser",
                NOW
        );

        return new ClaimedOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                KafkaTopics.TRADE_EXECUTED,
                objectMapper.writeValueAsString(dto)
        );
    }

    private ClaimedOutboxEvent snapshotEvent() throws Exception {
        PortfolioSnapshotEvent dto = new PortfolioSnapshotEvent(
                USER_ID,
                ACCOUNT_ID,
                SEASON_ID,
                1,
                "testUser",
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                1,
                0,
                NOW
        );

        return new ClaimedOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                KafkaTopics.PORTFOLIO_SNAPSHOT,
                objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    @DisplayName("선점된 이벤트가 없으면 Kafka 발행을 수행하지 않는다.")
    void noClaimedEvents() {
        when(claimService.claimPublishableEvents())
                .thenReturn(List.of());

        scheduler.publishPendingEvents();

        verify(kafkaProducer, never()).sendTradeExecuted(any());
        verify(kafkaProducer, never()).sendPortfolioSnapshot(any());

        verifyNoInteractions(resultService);
    }

    @Test
    @DisplayName("거래 이벤트 발행 성공 시 결과 저장 서비스를 호출한다.")
    void tradeEvent_success() throws Exception {
        ClaimedOutboxEvent event = tradeEvent();

        when(claimService.claimPublishableEvents())
                .thenReturn(List.of(event));

        when(kafkaProducer.sendTradeExecuted(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingEvents();

        ArgumentCaptor<TradeExecutedEvent> captor =
                ArgumentCaptor.forClass(TradeExecutedEvent.class);

        verify(kafkaProducer).sendTradeExecuted(captor.capture());

        assertThat(captor.getValue().userId())
                .isEqualTo(USER_ID);

        assertThat(captor.getValue().stockCode())
                .isEqualTo("005930");

        verify(resultService).markPublished(
                event.eventId(),
                event.processingToken()
        );

        verify(resultService, never())
                .markFailed(any(), any());
    }

    @Test
    @DisplayName("포트폴리오 이벤트 발행 성공 시 결과 저장 서비스를 호출한다.")
    void snapshotEvent_success() throws Exception {
        ClaimedOutboxEvent event = snapshotEvent();

        when(claimService.claimPublishableEvents())
                .thenReturn(List.of(event));

        when(kafkaProducer.sendPortfolioSnapshot(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingEvents();

        ArgumentCaptor<PortfolioSnapshotEvent> captor =
                ArgumentCaptor.forClass(PortfolioSnapshotEvent.class);

        verify(kafkaProducer).sendPortfolioSnapshot(captor.capture());

        assertThat(captor.getValue().accountId())
                .isEqualTo(ACCOUNT_ID);

        verify(resultService).markPublished(
                event.eventId(),
                event.processingToken()
        );
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 실패 결과 저장 서비스를 호출한다.")
    void kafkaFailure_marksFailed() throws Exception {
        ClaimedOutboxEvent event = tradeEvent();

        when(claimService.claimPublishableEvents())
                .thenReturn(List.of(event));

        when(kafkaProducer.sendTradeExecuted(any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka 브로커 연결 실패")
                ));

        scheduler.publishPendingEvents();

        verify(resultService).markFailed(
                event.eventId(),
                event.processingToken()
        );

        verify(resultService, never())
                .markPublished(any(), any());
    }

    @Test
    @DisplayName("첫 이벤트 발행 실패 후에도 다음 이벤트를 발행한다.")
    void firstFails_secondSucceeds() throws Exception {
        ClaimedOutboxEvent failedEvent = tradeEvent();
        ClaimedOutboxEvent successEvent = snapshotEvent();

        when(claimService.claimPublishableEvents())
                .thenReturn(List.of(failedEvent, successEvent));

        when(kafkaProducer.sendTradeExecuted(any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka 발행 실패")
                ));

        when(kafkaProducer.sendPortfolioSnapshot(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingEvents();

        verify(resultService).markFailed(
                failedEvent.eventId(),
                failedEvent.processingToken()
        );

        verify(resultService).markPublished(
                successEvent.eventId(),
                successEvent.processingToken()
        );

        verify(kafkaProducer).sendPortfolioSnapshot(any());
    }

    @Test
    @DisplayName("각 이벤트의 발행 성공 결과를 개별적으로 저장한다.")
    void multipleEvents_resultsRecorded() throws Exception {
        ClaimedOutboxEvent event1 = tradeEvent();
        ClaimedOutboxEvent event2 = snapshotEvent();

        when(claimService.claimPublishableEvents())
                .thenReturn(List.of(event1, event2));

        when(kafkaProducer.sendTradeExecuted(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(kafkaProducer.sendPortfolioSnapshot(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingEvents();

        verify(resultService).markPublished(
                event1.eventId(),
                event1.processingToken()
        );

        verify(resultService).markPublished(
                event2.eventId(),
                event2.processingToken()
        );

        verify(resultService, times(2))
                .markPublished(any(), any());
    }

    @Test
    @DisplayName("이벤트 선점 전에 최대 시도 횟수 초과 이벤트를 복구한다.")
    void publishPendingEvents_recoversBeforeClaim() {
        // given
        when(claimService.claimPublishableEvents())
                .thenReturn(List.of());

        // when
        scheduler.publishPendingEvents();

        // then
        InOrder inOrder = inOrder(
                recoveryService,
                claimService
        );

        inOrder.verify(recoveryService)
                .recoverExhaustedProcessingEvents();

        inOrder.verify(claimService)
                .claimPublishableEvents();
    }

}
