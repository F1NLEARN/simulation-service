package com.finlearn.simulationservice.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.investment.service.InvestmentService;
import com.finlearn.simulationservice.domain.investment.event.PointQuizPassedEvent;
import com.finlearn.simulationservice.infrastructure.client.SeasonServiceClient;
import com.finlearn.simulationservice.infrastructure.client.SeasonServiceClient.CurrentSeasonInfo;
import com.finlearn.simulationservice.infrastructure.client.UserServiceClient;
import com.finlearn.simulationservice.infrastructure.kafka.event.QuizGradedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationKafkaConsumer {

    private final InvestmentService investmentService;
    private final SeasonServiceClient seasonServiceClient;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.quiz.graded}", groupId = "simulation-service")
    public void handleQuizGraded(Map<String, Object> rawEvent) {
        QuizGradedKafkaEvent event = objectMapper.convertValue(rawEvent, QuizGradedKafkaEvent.class);
        log.info("[SimulationKafkaConsumer] quiz.graded 수신 - userId={}, seedMoney={}", event.userId(), event.seedMoney());

        CurrentSeasonInfo season = seasonServiceClient.getCurrentSeason().orElse(null);
        if (season == null) {
            log.error("[SimulationKafkaConsumer] 현재 시즌 조회 실패 - 계좌 생성 불가 userId={}", event.userId());
            return;
        }

        String investorName = userServiceClient.getNickname(event.userId())
                .orElse(event.userId().toString());

        PointQuizPassedEvent domainEvent = new PointQuizPassedEvent(
                season.seasonId(),
                season.seasonNumber(),
                event.userId(),
                investorName,
                event.seedMoney()
        );

        investmentService.handlePointQuizPassed(domainEvent);
        log.info("[SimulationKafkaConsumer] 계좌 생성 완료 - userId={}, seasonId={}", event.userId(), season.seasonId());
    }
}