package com.finlearn.simulationservice.application.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.producer.SimulationKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventSchedulerTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private SimulationKafkaProducer kafkaProducer;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventScheduler scheduler;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 18, 10, 0);

    private OutboxEvent pendingTradeEvent() throws Exception {
        TradeExecutedEvent dto = new TradeExecutedEvent(
                USER_ID, ACCOUNT_ID, SEASON_ID, "BUY", "STOCK", "005930", NOW);
        return OutboxEvent.of(KafkaTopics.TRADE_EXECUTED, objectMapper.writeValueAsString(dto));
    }

    private OutboxEvent pendingSnapshotEvent() throws Exception {
        PortfolioSnapshotEvent dto = new PortfolioSnapshotEvent(
                USER_ID, ACCOUNT_ID, SEASON_ID,
                new BigDecimal("5.00"), new BigDecimal("10.00"), BigDecimal.ZERO,
                1, 0, NOW);
        return OutboxEvent.of(KafkaTopics.PORTFOLIO_SNAPSHOT, objectMapper.writeValueAsString(dto));
    }

    // ===== 정상 발행 =====

    @Test
    @DisplayName("PENDING 이벤트가 없으면 Kafka 발행이 일어나지 않는다.")
    void publishPendingEvents_noPending_nothingPublished() {
        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of());

        scheduler.publishPendingEvents();

        verify(kafkaProducer, never()).sendTradeExecuted(any());
        verify(kafkaProducer, never()).sendPortfolioSnapshot(any());
    }

    @Test
    @DisplayName("PENDING trade.executed 이벤트가 Kafka로 발행된다.")
    void publishPendingEvents_tradeExecuted_publishedToKafka() throws Exception {
        OutboxEvent event = pendingTradeEvent();
        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.publishPendingEvents();

        ArgumentCaptor<TradeExecutedEvent> captor = ArgumentCaptor.forClass(TradeExecutedEvent.class);
        verify(kafkaProducer).sendTradeExecuted(captor.capture());

        TradeExecutedEvent published = captor.getValue();
        assertThat(published.userId()).isEqualTo(USER_ID);
        assertThat(published.tradeType()).isEqualTo("BUY");
        assertThat(published.assetType()).isEqualTo("STOCK");
        assertThat(published.stockCode()).isEqualTo("005930");
    }

    @Test
    @DisplayName("PENDING portfolio.snapshot 이벤트가 Kafka로 발행된다.")
    void publishPendingEvents_portfolioSnapshot_publishedToKafka() throws Exception {
        OutboxEvent event = pendingSnapshotEvent();
        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.publishPendingEvents();

        ArgumentCaptor<PortfolioSnapshotEvent> captor = ArgumentCaptor.forClass(PortfolioSnapshotEvent.class);
        verify(kafkaProducer).sendPortfolioSnapshot(captor.capture());

        PortfolioSnapshotEvent published = captor.getValue();
        assertThat(published.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(published.stockReturnRate()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Kafka 발행 성공 후 status가 PUBLISHED로 변경된다.")
    void publishPendingEvents_success_statusBecomesPublished() throws Exception {
        OutboxEvent event = pendingTradeEvent();
        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Kafka 발행 성공 후 publishedAt이 기록된다.")
    void publishPendingEvents_success_publishedAtRecorded() throws Exception {
        OutboxEvent event = pendingTradeEvent();
        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.publishPendingEvents();

        assertThat(event.getPublishedAt()).isNotNull().isBefore(LocalDateTime.now().plusSeconds(1));
    }

    // ===== 실패 처리 =====

    @Test
    @DisplayName("Kafka 발행 실패 시 status가 FAILED로 변경된다.")
    void publishPendingEvents_kafkaFails_statusBecomesFailed() throws Exception {
        OutboxEvent event = pendingTradeEvent();
        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Kafka 브로커 연결 실패")).when(kafkaProducer).sendTradeExecuted(any());

        scheduler.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    }

    @Test
    @DisplayName("첫 번째 이벤트 발행 실패가 두 번째 이벤트 처리를 중단시키지 않는다.")
    void publishPendingEvents_firstFails_secondStillProcessed() throws Exception {
        OutboxEvent failEvent = pendingTradeEvent();
        OutboxEvent successEvent = pendingSnapshotEvent();

        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING))
                .thenReturn(List.of(failEvent, successEvent));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Kafka 오류")).when(kafkaProducer).sendTradeExecuted(any());

        scheduler.publishPendingEvents();

        assertThat(failEvent.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(successEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        verify(kafkaProducer).sendPortfolioSnapshot(any());
    }

    @Test
    @DisplayName("발행 후 각 이벤트의 상태가 outbox에 저장된다.")
    void publishPendingEvents_saveCalledForEachEvent() throws Exception {
        OutboxEvent event1 = pendingTradeEvent();
        OutboxEvent event2 = pendingSnapshotEvent();
        when(outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING))
                .thenReturn(List.of(event1, event2));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.publishPendingEvents();

        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }
}