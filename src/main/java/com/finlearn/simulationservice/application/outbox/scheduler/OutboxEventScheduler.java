package com.finlearn.simulationservice.application.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.producer.SimulationKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(SimulationKafkaProducer.class)
public class OutboxEventScheduler {

    private final OutboxEventClaimService claimService;
    private final OutboxEventResultService resultService;
    private final OutboxEventRecoveryService recoveryService;
    private final SimulationKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    /**
     * Outbox 이벤트 발행 스케줄러.
     *
     * 1. 최대 시도 횟수에 도달한 만료 PROCESSING 이벤트 정리
     * 2. 발행 가능한 이벤트를 별도 트랜잭션에서 선점
     * 3. Kafka 발행 완료 확인
     * 4. 발행 결과를 별도 트랜잭션에서 저장
     *
     * DB 비관적 잠금으로 동시 선점을 제어하지만,
     * 장애 상황에서는 Kafka 메시지가 중복 발행될 수 있다.
     */
    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        // 최대 시도 횟수를 모두 사용한 타임아웃 이벤트 정리
        recoveryService.recoverExhaustedProcessingEvents();

        // 발행 가능한 이벤트 선점
        List<ClaimedOutboxEvent> events =
                claimService.claimPublishableEvents();

        for (ClaimedOutboxEvent event : events) {

            try {
                // Kafka 발행만 별도로 시도
                dispatch(event);

            } catch (Exception e) {
                log.error(
                        "Outbox 이벤트 발행 실패 [id={}, topic={}]",
                        event.eventId(),
                        event.topic(),
                        e
                );

                // 실패 결과 저장은 별도 트랜잭션
                resultService.markFailed(
                        event.eventId(),
                        event.processingToken()
                );

                continue;
            }

            // Kafka 발행이 성공했을 때만 성공 결과 저장
            resultService.markPublished(
                    event.eventId(),
                    event.processingToken()
            );
        }
    }

    private void dispatch(ClaimedOutboxEvent event) throws Exception {
        String topic = event.topic();
        String payload = event.payload();

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