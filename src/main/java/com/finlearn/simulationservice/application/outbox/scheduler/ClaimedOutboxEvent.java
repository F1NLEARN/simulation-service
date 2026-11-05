package com.finlearn.simulationservice.application.outbox.scheduler;

import java.util.UUID;

public record ClaimedOutboxEvent(
        UUID eventId,
        UUID processingToken,
        String topic,   // Kafka 발행 토픽
        String payload  // Kafka 메시지 데이터
) {
}
