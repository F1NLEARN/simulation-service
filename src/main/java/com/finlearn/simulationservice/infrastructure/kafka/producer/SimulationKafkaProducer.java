package com.finlearn.simulationservice.infrastructure.kafka.producer;

import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class SimulationKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> sendTradeExecuted(TradeExecutedEvent event) {
        return kafkaTemplate.send(KafkaTopics.TRADE_EXECUTED, event.accountId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> sendPortfolioSnapshot(PortfolioSnapshotEvent event) {
        return kafkaTemplate.send(KafkaTopics.PORTFOLIO_SNAPSHOT, event.accountId().toString(), event);
    }
}