package com.finlearn.simulationservice.infrastructure.outbox.persistence;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID>, OutboxEventRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status")
    List<OutboxEvent> findAllByStatusForUpdate(@Param("status") OutboxEventStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT e FROM OutboxEvent e
    WHERE e.attemptCount < :maxAttempts
      AND (
          e.status = com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus.PENDING
          OR (
              e.status = com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus.FAILED
              AND e.nextRetryAt <= :now
          )
          OR (
              e.status = com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus.PROCESSING
              AND e.processingStartedAt <= :expiredBefore
          )
      )
    ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findPublishableEventsForUpdate(
            @Param("now") LocalDateTime now,
            @Param("expiredBefore") LocalDateTime expiredBefore,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.id = :id")
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("id") UUID id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT e FROM OutboxEvent e
    WHERE e.status = com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus.PROCESSING
      AND e.attemptCount >= :maxAttempts
      AND e.processingStartedAt <= :expiredBefore
    ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findExhaustedProcessingEventsForUpdate(
            @Param("expiredBefore") LocalDateTime expiredBefore,
            @Param("maxAttempts") int maxAttempts
    );

}