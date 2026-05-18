package com.finlearn.simulationservice.infrastructure.kafka.producer;

import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class SimulationKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTradeExecuted(TradeExecutedEvent event) {
        kafkaTemplate.send(KafkaTopics.TRADE_EXECUTED, event.accountId().toString(), event);
    }

    public void sendPortfolioSnapshot(PortfolioSnapshotEvent event) {
        kafkaTemplate.send(KafkaTopics.PORTFOLIO_SNAPSHOT, event.accountId().toString(), event);
    }
}