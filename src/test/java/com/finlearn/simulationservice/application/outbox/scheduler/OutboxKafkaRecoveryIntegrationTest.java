package com.finlearn.simulationservice.application.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEventStatus;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import com.finlearn.simulationservice.infrastructure.kafka.producer.SimulationKafkaProducer;
import com.finlearn.simulationservice.infrastructure.outbox.persistence.OutboxEventJpaRepository;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        OutboxEventClaimService.class,
        OutboxEventResultService.class,
        OutboxEventRecoveryService.class
})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxKafkaRecoveryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer("apache/kafka:3.7.0");

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
    private OutboxEventResultService resultService;

    @Autowired
    private OutboxEventRecoveryService recoveryService;

    private DefaultKafkaProducerFactory<String, Object> producerFactory;
    private SimulationKafkaProducer kafkaProducer;
    private OutboxEventScheduler scheduler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        repository.deleteAll();

        objectMapper = new ObjectMapper().findAndRegisterModules();

        // 테스트 전용 토픽을 실제 Kafka에 생성
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        ))) {
            admin.createTopics(List.of(
                    new NewTopic(
                            KafkaTopics.TRADE_EXECUTED,
                            1,
                            (short) 1
                    )
            )).all().get(15, TimeUnit.SECONDS);
        }

        // 실제 KafkaTemplate 생성
        Map<String, Object> producerProperties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ProducerConfig.ACKS_CONFIG,
                "all",
                ProducerConfig.RETRIES_CONFIG,
                0,
                ProducerConfig.MAX_BLOCK_MS_CONFIG,
                3000,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                3000,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                10000
        );

        producerFactory = new DefaultKafkaProducerFactory<>(
                producerProperties,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper)
        );

        KafkaTemplate<String, Object> kafkaTemplate =
                new KafkaTemplate<>(producerFactory);

        kafkaProducer = new SimulationKafkaProducer(kafkaTemplate);

        // @Scheduled 자동 실행 없이 직접 호출
        scheduler = new OutboxEventScheduler(
                claimService,
                resultService,
                recoveryService,
                kafkaProducer,
                objectMapper
        );

        // 장애 발생 전 Producer 연결 및 메타데이터 준비
        TradeExecutedEvent warmupEvent =
                createTradeEvent(UUID.randomUUID());

        kafkaProducer.sendTradeExecuted(warmupEvent)
                .get(10, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() {
        if (producerFactory != null) {
            producerFactory.destroy();
        }
    }

    @Test
    @DisplayName("Kafka 장애 후 FAILED 이벤트가 복구되면 자동 재발행된다.")
    void kafkaFailureThenRecovery_republishesFailedEvent()
            throws Exception {

        // given: 정상적인 거래 이벤트 생성
        UUID accountId = UUID.randomUUID();

        TradeExecutedEvent tradeEvent =
                createTradeEvent(accountId);

        String payload =
                objectMapper.writeValueAsString(tradeEvent);

        OutboxEvent outboxEvent =
                OutboxEvent.of(
                        KafkaTopics.TRADE_EXECUTED,
                        payload
                );

        UUID eventId =
                repository.saveAndFlush(outboxEvent).getId();

        assertThat(repository.findById(eventId).orElseThrow()
                .getStatus())
                .isEqualTo(OutboxEventStatus.PENDING);

        // when: Kafka 장애 발생
        DockerClientFactory.instance()
                .client()
                .pauseContainerCmd(kafka.getContainerId())
                .exec();

        try {
            // 실제 Kafka 발행 시도
            scheduler.publishPendingEvents();

        } finally {
            // 실패하더라도 Kafka 컨테이너는 반드시 복구
            DockerClientFactory.instance()
                    .client()
                    .unpauseContainerCmd(kafka.getContainerId())
                    .exec();
        }

        // then: 실패 상태가 PostgreSQL에 저장됐는지 확인
        OutboxEvent failed =
                repository.findById(eventId).orElseThrow();

        assertThat(failed.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(failed.getRetryCount())
                .isEqualTo(1);

        assertThat(failed.getAttemptCount())
                .isEqualTo(1);

        assertThat(failed.getNextRetryAt())
                .isNotNull();

        assertThat(failed.getProcessingToken())
                .isNull();

        // 재시도 예정 시각까지 대기
        long waitMillis = Duration.between(
                LocalDateTime.now(),
                failed.getNextRetryAt()
        ).toMillis();

        Thread.sleep(Math.max(0, waitMillis) + 500);

        // when: 복구된 Kafka에 기존 FAILED 이벤트 재발행
        scheduler.publishPendingEvents();

        // then: 동일 Outbox 이벤트가 PUBLISHED로 변경
        OutboxEvent published =
                repository.findById(eventId).orElseThrow();

        assertThat(published.getStatus())
                .isEqualTo(OutboxEventStatus.PUBLISHED);

        assertThat(published.getAttemptCount())
                .isEqualTo(2);

        assertThat(published.getRetryCount())
                .isEqualTo(1);

        assertThat(published.getPublishedAt())
                .isNotNull();

        assertThat(published.getProcessingToken())
                .isNull();

        // 실제 Kafka 토픽에도 메시지가 기록됐는지 검증
        assertKafkaMessageExists(accountId);
    }

    private TradeExecutedEvent createTradeEvent(UUID accountId) {
        return new TradeExecutedEvent(
                UUID.randomUUID(),
                accountId,
                UUID.randomUUID(),
                1,
                "BUY",
                "STOCK",
                "005930",
                10,
                2.5,
                2.5,
                2.5,
                0.0,
                "outbox-test-user",
                LocalDateTime.now()
        );
    }

    private void assertKafkaMessageExists(UUID accountId) {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "outbox-recovery-test-" + UUID.randomUUID()
        );
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        boolean found = false;

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of(KafkaTopics.TRADE_EXECUTED));

            long deadline =
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(15);

            while (System.nanoTime() < deadline && !found) {
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    if (accountId.toString().equals(record.key())
                            && record.value().contains(
                            accountId.toString()
                    )) {
                        found = true;
                        break;
                    }
                }
            }
        }

        assertThat(found)
                .as("복구 후 Kafka에 해당 accountId의 이벤트가 발행되어야 한다.")
                .isTrue();
    }
}
