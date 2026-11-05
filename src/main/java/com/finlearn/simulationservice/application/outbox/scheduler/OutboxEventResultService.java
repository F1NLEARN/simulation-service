package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventResultService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public boolean markPublished(UUID eventId, UUID token) {
        OutboxEvent event = outboxEventRepository
                .findByIdForUpdate(eventId)
                .orElse(null);

        if (event == null) {
            return false;
        }

        return event.markPublished(token);
    }

    @Transactional
    public boolean markFailed(UUID eventId, UUID token) {
        OutboxEvent event = outboxEventRepository
                .findByIdForUpdate(eventId)
                .orElse(null);

        if (event == null) {
            return false;
        }

        return event.markFailed(token);
    }
}
