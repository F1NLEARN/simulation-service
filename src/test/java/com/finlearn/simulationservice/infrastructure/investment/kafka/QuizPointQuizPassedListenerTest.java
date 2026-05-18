package com.finlearn.simulationservice.infrastructure.investment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.investment.service.InvestmentService;
import com.finlearn.simulationservice.domain.investment.event.PointQuizPassedEvent;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuizPointQuizPassedListenerTest {

    @Mock
    private InvestmentService investmentService;

    private QuizPointQuizPassedListener listener;

    @BeforeEach
    void setUp() {
        listener = new QuizPointQuizPassedListener(investmentService, new ObjectMapper());
    }

    @Test
    void handleQuizGradedCreatesInvestmentAccountFromMapPayload() {
        UUID userId = UUID.randomUUID();
        long seedMoney = 1_000_000L;

        listener.handleQuizGraded(new ConsumerRecord<>("local-quiz-graded", 0, 0, null, Map.of(
                "userId", userId.toString(),
                "seedMoney", seedMoney,
                "endedAt", "2026-05-18T12:00:00+09:00"
        )));

        ArgumentCaptor<PointQuizPassedEvent> eventCaptor = ArgumentCaptor.forClass(PointQuizPassedEvent.class);
        verify(investmentService).handlePointQuizPassed(eventCaptor.capture());

        PointQuizPassedEvent event = eventCaptor.getValue();
        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), event.seasonId());
        assertEquals(1, event.seasonNumber());
        assertEquals(userId, event.investorId());
        assertEquals("투자자", event.investorName());
        assertEquals(seedMoney, event.seedMoney());
    }

    @Test
    void handleQuizGradedCreatesInvestmentAccountFromJsonStringPayload() {
        UUID userId = UUID.randomUUID();
        long seedMoney = 1_000_000L;

        listener.handleQuizGraded(new ConsumerRecord<>("local-quiz-graded", 0, 0, null, """
                {"userId":"%s","seedMoney":%d,"endedAt":"2026-05-18T12:00:00+09:00"}
                """.formatted(userId, seedMoney)));

        ArgumentCaptor<PointQuizPassedEvent> eventCaptor = ArgumentCaptor.forClass(PointQuizPassedEvent.class);
        verify(investmentService).handlePointQuizPassed(eventCaptor.capture());

        PointQuizPassedEvent event = eventCaptor.getValue();
        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), event.seasonId());
        assertEquals(1, event.seasonNumber());
        assertEquals(userId, event.investorId());
        assertEquals("투자자", event.investorName());
        assertEquals(seedMoney, event.seedMoney());
    }
}
