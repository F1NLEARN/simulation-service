package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long PROCESSING_TIMEOUT_SECONDS = 120;
    private static final int BATCH_SIZE = 3;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<ClaimedOutboxEvent> claimPublishableEvents() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredBefore =
                now.minusSeconds(PROCESSING_TIMEOUT_SECONDS);

        // 발행 가능한 이벤트 조회 및 DB 비관적 락 획득
        List<OutboxEvent> events =
                outboxEventRepository.findPublishableEventsForUpdate(
                        now,
                        expiredBefore,
                        MAX_ATTEMPTS,
                        PageRequest.of(0, BATCH_SIZE)
                );

        // PROCESSING 상태로 선점
        for (OutboxEvent event : events) {
            event.markProcessing(now);
        }

        // 트랜잭션 밖에서 사용할 데이터만 반환
        return events.stream()
                .map(event -> new ClaimedOutboxEvent(
                        event.getId(),
                        event.getProcessingToken(),
                        event.getTopic(),
                        event.getPayload()
                ))
                .toList();
    }
}