package com.finlearn.simulationservice.application.outbox.scheduler;

import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.infrastructure.outbox.persistence.OutboxEventJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(OutboxEventClaimService.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxEventConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );
        registry.add(
                "spring.jpa.database-platform",
                () -> "org.hibernate.dialect.PostgreSQLDialect"
        );
    }

    @Autowired
    private OutboxEventJpaRepository repository;

    @Autowired
    private OutboxEventClaimService claimService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("두 스레드가 동시에 선점해도 동일 이벤트는 한 번만 선점한다.")
    void concurrentClaim_claimsEventOnlyOnce() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.of(
                "test.concurrent", "{}"
        );

        UUID eventId = repository.saveAndFlush(event).getId();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            java.util.concurrent.Callable<List<ClaimedOutboxEvent>>
                    task = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "동시 실행 시작 대기 시간 초과"
                    );
                }

                return claimService.claimPublishableEvents();
            };

            Future<List<ClaimedOutboxEvent>> future1 =
                    executor.submit(task);

            Future<List<ClaimedOutboxEvent>> future2 =
                    executor.submit(task);

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .isTrue();

            // when: 두 스레드를 동시에 출발시킨다.
            start.countDown();

            List<ClaimedOutboxEvent> result1 =
                    future1.get(20, TimeUnit.SECONDS);

            List<ClaimedOutboxEvent> result2 =
                    future2.get(20, TimeUnit.SECONDS);

            // then: 전체 선점 결과는 정확히 1건
            assertThat(result1.size() + result2.size())
                    .isEqualTo(1);

            // DB 상태 검증
            OutboxEvent saved = repository.findById(eventId)
                    .orElseThrow();

            assertThat(saved.getStatus())
                    .isEqualTo(OutboxEventStatus.PROCESSING);

            assertThat(saved.getAttemptCount())
                    .isEqualTo(1);

            assertThat(saved.getProcessingToken())
                    .isNotNull();

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("첫 트랜잭션이 잠금을 보유한 동안 다른 선점은 대기하고, 커밋 후 중복 선점하지 않는다.")
    void concurrentClaim_waitsForLockAndDoesNotReclaim() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.of(
                "test.lock", "{}"
        );

        UUID eventId = repository.saveAndFlush(event).getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        try {
            // 트랜잭션 A: 행 잠금 획득 후 커밋을 지연
            Future<?> first = executor.submit(() -> {
                TransactionTemplate tx =
                        new TransactionTemplate(transactionManager);

                tx.executeWithoutResult(status -> {
                    OutboxEvent locked = repository
                            .findByIdForUpdate(eventId)
                            .orElseThrow();

                    locked.markProcessing();

                    // A가 잠금을 확보했다는 신호
                    lockAcquired.countDown();

                    try {
                        if (!releaseLock.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "잠금 해제 대기 시간 초과"
                            );
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                });
            });

            // A가 실제로 잠금을 얻었는지 확인
            assertThat(lockAcquired.await(10, TimeUnit.SECONDS))
                    .isTrue();

            // 트랜잭션 B: 동일한 PENDING 이벤트 선점 시도
            Future<List<ClaimedOutboxEvent>> second =
                    executor.submit(() -> {
                        secondStarted.countDown();
                        return claimService.claimPublishableEvents();
                    });

            assertThat(secondStarted.await(10, TimeUnit.SECONDS))
                    .isTrue();

            // A가 잠금을 보유 중인 동안 B가 완료되는지 관찰
            try {
                second.get(300, TimeUnit.MILLISECONDS);

                throw new AssertionError(
                        "잠금 해제 전에 두 번째 선점이 완료되었습니다."
                );
            } catch (TimeoutException expected) {
                // B가 아직 완료되지 않은 상태
            }

            // when: 트랜잭션 A가 커밋할 수 있도록 잠금 해제
            releaseLock.countDown();

            first.get(10, TimeUnit.SECONDS);

            List<ClaimedOutboxEvent> secondResult =
                    second.get(10, TimeUnit.SECONDS);

            // then: B는 이미 선점된 이벤트를 가져오지 않음
            assertThat(secondResult).isEmpty();

            OutboxEvent saved = repository.findById(eventId)
                    .orElseThrow();

            assertThat(saved.getStatus())
                    .isEqualTo(OutboxEventStatus.PROCESSING);

            assertThat(saved.getAttemptCount())
                    .isEqualTo(1);

            assertThat(saved.getProcessingToken())
                    .isNotNull();

        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("PostgreSQL에서 8개 이벤트를 3개씩 나누어 선점한다.")
    void claimPublishableEvents_respectsBatchSize() {
        // given
        List<OutboxEvent> events = new java.util.ArrayList<>();

        for (int i = 0; i < 8; i++) {
            events.add(OutboxEvent.of(
                    "test.batch." + i,
                    "{}"
            ));
        }

        repository.saveAllAndFlush(events);

        // when
        List<ClaimedOutboxEvent> firstBatch =
                claimService.claimPublishableEvents();

        List<ClaimedOutboxEvent> secondBatch =
                claimService.claimPublishableEvents();

        List<ClaimedOutboxEvent> thirdBatch =
                claimService.claimPublishableEvents();

        List<ClaimedOutboxEvent> fourthBatch =
                claimService.claimPublishableEvents();

        // then
        assertThat(firstBatch).hasSize(3);
        assertThat(secondBatch).hasSize(3);
        assertThat(thirdBatch).hasSize(2);
        assertThat(fourthBatch).isEmpty();

        List<UUID> allClaimedIds =
                java.util.stream.Stream.of(
                                firstBatch,
                                secondBatch,
                                thirdBatch
                        )
                        .flatMap(List::stream)
                        .map(ClaimedOutboxEvent::eventId)
                        .toList();

        // 중복 선점이 없어야 함
        assertThat(allClaimedIds)
                .doesNotHaveDuplicates();

        assertThat(allClaimedIds).hasSize(8);

        // DB 상태 검증
        List<OutboxEvent> saved = repository.findAll();

        assertThat(saved).hasSize(8);

        assertThat(saved)
                .allMatch(event ->
                        event.getStatus() == OutboxEventStatus.PROCESSING
                                && event.getAttemptCount() == 1
                                && event.getProcessingToken() != null
                );
    }



}
