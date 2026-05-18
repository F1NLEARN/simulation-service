package com.finlearn.simulationservice.infrastructure.kafka.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * quiz-service가 kafka quiz.graded 토픽에 발행하는 이벤트 DTO.
 * quiz-service의 PointQuizPassedEvent와 동일한 구조를 유지해야 한다.
 */
public record QuizGradedKafkaEvent(
        UUID userId,
        long seedMoney,
        OffsetDateTime endedAt
) {
}