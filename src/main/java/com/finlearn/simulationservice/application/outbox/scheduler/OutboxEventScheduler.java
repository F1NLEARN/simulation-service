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

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllByStatus(OutboxEventStatus.PENDING);

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