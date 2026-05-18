package com.finlearn.simulationservice.infrastructure.investment.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.investment.service.InvestmentService;
import com.finlearn.simulationservice.domain.investment.event.PointQuizPassedEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizPointQuizPassedListener {

    // TODO: Season 도메인 연동 후 실제 seasonId/seasonNumber/investorName를 조회하도록 변경
    private static final UUID MVP_DEFAULT_SEASON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int MVP_DEFAULT_SEASON_NUMBER = 1;
    private static final String DEFAULT_INVESTOR_NAME = "투자자";

    private final InvestmentService investmentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.quiz.graded}",
            groupId = "${spring.kafka.consumer.group-id:simulation-service}"
    )
    public void handleQuizGraded(ConsumerRecord<String, Object> record) {
        Map<String, Object> eventPayload = parsePayload(record.value());
        UUID userId = readUuid(eventPayload, "userId");
        long seedMoney = readLong(eventPayload, "seedMoney");

        investmentService.handlePointQuizPassed(new PointQuizPassedEvent(
                MVP_DEFAULT_SEASON_ID,
                MVP_DEFAULT_SEASON_NUMBER,
                userId,
                DEFAULT_INVESTOR_NAME,
                seedMoney
        ));

        log.info("[Kafka] 포인트 퀴즈 PASS 계좌 생성 처리 완료 - userId={}, seedMoney={}", userId, seedMoney);
    }

    private Map<String, Object> parsePayload(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Map<String, Object> parsed = new LinkedHashMap<>();
            map.forEach((key, value) -> parsed.put(String.valueOf(key), value));
            return parsed;
        }
        if (payload instanceof String text) {
            try {
                return objectMapper.readValue(text, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalArgumentException("Kafka payload JSON 파싱에 실패했습니다.", e);
            }
        }
        throw new IllegalArgumentException("지원하지 않는 Kafka payload 타입입니다: " + payload.getClass().getName());
    }

    private UUID readUuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text) {
            return UUID.fromString(text);
        }
        throw new IllegalArgumentException("Kafka payload에 유효한 " + key + " 값이 없습니다.");
    }

    private long readLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("Kafka payload에 유효한 " + key + " 값이 없습니다.");
    }
}
