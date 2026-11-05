package com.finlearn.simulationservice.domain.outbox.repository;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    List<OutboxEvent> findAllByStatus(OutboxEventStatus status);

    List<OutboxEvent> findAllByStatusForUpdate(OutboxEventStatus status);

    Optional<OutboxEvent> findByIdForUpdate(UUID id);

    List<OutboxEvent> findPublishableEventsForUpdate(
            LocalDateTime now,
            LocalDateTime expiredBefore,
            int maxAttempts,
            Pageable pageable
    );

    List<OutboxEvent> findExhaustedProcessingEventsForUpdate(
            LocalDateTime expiredBefore,
            int maxAttempts
    );

}