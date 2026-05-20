package com.finlearn.simulationservice.application.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.producer.SimulationKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(SimulationKafkaProducer.class)
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final SimulationKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    /**
     * PENDING 이벤트를 PROCESSING으로 선점(비관적 잠금)한 뒤 Kafka 발행.
     * - PESSIMISTIC_WRITE 잠금으로 다중 인스턴스 중복 발행 방지
     * - kafkaTemplate.send() get(10s)으로 브로커 전송 완료 확인 후 PUBLISHED 처리
     * - 알 수 없는 토픽은 예외를 던져 FAILED로 저장 (PENDING 영구화 방지)
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllByStatusForUpdate(OutboxEventStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            event.markProcessing();
            outboxEventRepository.save(event);
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                dispatch(event);
                event.markPublished();
            } catch (Exception e) {
                log.error("Outbox 이벤트 발행 실패 [id={}, topic={}]: {}", event.getId(), event.getTopic(), e.getMessage());
                event.markFailed();
            }
            outboxEventRepository.save(event);
        }
    }

    private void dispatch(OutboxEvent event) throws Exception {
        String topic = event.getTopic();
        String payload = event.getPayload();

        if (KafkaTopics.TRADE_EXECUTED.equals(topic)) {
            kafkaProducer.sendTradeExecuted(objectMapper.readValue(payload, TradeExecutedEvent.class))
                    .get(10, TimeUnit.SECONDS);
        } else if (KafkaTopics.PORTFOLIO_SNAPSHOT.equals(topic)) {
            kafkaProducer.sendPortfolioSnapshot(objectMapper.readValue(payload, PortfolioSnapshotEvent.class))
                    .get(10, TimeUnit.SECONDS);
        } else {
            throw new IllegalArgumentException("처리할 수 없는 Outbox 토픽: " + topic);
        }
    }
}