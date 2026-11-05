package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventRecoveryService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long PROCESSING_TIMEOUT_SECONDS = 120;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public int recoverExhaustedProcessingEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredBefore =
                now.minusSeconds(PROCESSING_TIMEOUT_SECONDS);

        List<OutboxEvent> events =
                outboxEventRepository.findExhaustedProcessingEventsForUpdate(
                        expiredBefore,
                        MAX_ATTEMPTS
                );

        int recoveredCount = 0;

        for (OutboxEvent event : events) {
            boolean recovered =
                    event.markFailedIfProcessingExhausted(
                            now,
                            PROCESSING_TIMEOUT_SECONDS,
                            MAX_ATTEMPTS
                    );

            if (recovered) {
                recoveredCount++;
            }
        }

        return recoveredCount;
    }
}
