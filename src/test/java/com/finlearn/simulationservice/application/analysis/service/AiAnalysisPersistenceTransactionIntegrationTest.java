package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisType;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AiAnalysisPersistenceTransactionIntegrationTest {

    @Autowired
    private AiAnalysisPersistenceService persistenceService;

    @Autowired
    private AiAnalysisRepository aiAnalysisRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private CreateAiAnalysisCommand createCommand(UUID accountId) {
        LocalDateTime now = LocalDateTime.now();

        return new CreateAiAnalysisCommand(
                accountId,
                UUID.randomUUID(),
                "트랜잭션 테스트 사용자",
                UUID.randomUUID(),
                1,
                AnalysisType.PORTFOLIO,
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(50),
                "DOMESTIC_ETF",
                "포트폴리오 진단 결과",
                "AI 분석 결과",
                now.toLocalDate().atStartOfDay(),
                now,
                now,
                "테스트 프롬프트",
                "테스트 AI 응답"
        );
    }

    @Test
    @DisplayName("정상 AI 분석 이력이 COMPLETED 상태로 DB에 커밋된다.")
    void saveCompleted_commitsToDatabase() {

        // given
        UUID accountId = UUID.randomUUID();
        CreateAiAnalysisCommand command = createCommand(accountId);

        // when
        persistenceService.saveCompleted(command);

        // then: 저장 트랜잭션이 종료된 뒤 다시 조회
        List<AiAnalysis> saved =
                aiAnalysisRepository.findAllByAccountId(accountId);

        assertThat(saved).hasSize(1);

        AiAnalysis analysis = saved.get(0);

        assertThat(analysis.getAnalysisStatus())
                .isEqualTo(AnalysisStatus.COMPLETED);

        assertThat(analysis.getAiFeedbackMessage())
                .isEqualTo("AI 분석 결과");

        assertThat(analysis.getFailureReason())
                .isNull();
    }

    @Test
    @DisplayName("AI 실패 이력이 FAILED 상태로 DB에 커밋된다.")
    void saveFailed_commitsToDatabase() {

        // given
        UUID accountId = UUID.randomUUID();
        CreateAiAnalysisCommand command = createCommand(accountId);

        // when
        persistenceService.saveFailed(
                command,
                "AI API timeout"
        );

        // then
        List<AiAnalysis> saved =
                aiAnalysisRepository.findAllByAccountId(accountId);

        assertThat(saved).hasSize(1);

        AiAnalysis analysis = saved.get(0);

        assertThat(analysis.getAnalysisStatus())
                .isEqualTo(AnalysisStatus.FAILED);

        assertThat(analysis.getFailureReason())
                .isEqualTo("AI API timeout");
    }

    @Test
    @DisplayName("외부 트랜잭션이 롤백되어도 REQUIRES_NEW 분석 이력은 유지된다.")
    void saveFailed_requiresNew_commitsDespiteOuterRollback() {

        // given
        UUID accountId = UUID.randomUUID();
        CreateAiAnalysisCommand command = createCommand(accountId);

        AtomicReference<Boolean> outerTransactionActive =
                new AtomicReference<>();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        // when: 외부 트랜잭션 시작
        transactionTemplate.executeWithoutResult(status -> {

            outerTransactionActive.set(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            // 별도 REQUIRES_NEW 트랜잭션에서 저장
            persistenceService.saveFailed(
                    command,
                    "외부 트랜잭션 롤백 테스트"
            );

            // 외부 트랜잭션만 롤백
            status.setRollbackOnly();
        });

        // then 1: 외부 트랜잭션이 실제로 존재했는지 확인
        assertThat(outerTransactionActive.get())
                .as("외부 트랜잭션이 활성화되어야 합니다.")
                .isTrue();

        // then 2: 외부 트랜잭션 롤백 후에도 저장 이력 유지
        List<AiAnalysis> saved =
                aiAnalysisRepository.findAllByAccountId(accountId);

        assertThat(saved).hasSize(1);

        assertThat(saved.get(0).getAnalysisStatus())
                .isEqualTo(AnalysisStatus.FAILED);

        assertThat(saved.get(0).getFailureReason())
                .isEqualTo("외부 트랜잭션 롤백 테스트");
    }

    @Test
    @DisplayName("DB 제약조건 위반으로 저장이 실패하면 분석 이력이 남지 않는다.")
    void saveCompleted_constraintViolation_doesNotCommit() {

        // given: 테스트마다 고유한 계좌 ID 사용
        UUID accountId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // targetUserName 컬럼의 최대 길이(20자)를 초과하는 값
        String invalidUserName = "123456789012345678901";

        CreateAiAnalysisCommand command = new CreateAiAnalysisCommand(
                accountId,
                UUID.randomUUID(),
                invalidUserName,
                UUID.randomUUID(),
                1,
                AnalysisType.PORTFOLIO,
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(50),
                "DOMESTIC_ETF",
                "포트폴리오 진단 결과",
                "AI 분석 결과",
                now.toLocalDate().atStartOfDay(),
                now,
                now,
                "테스트 프롬프트",
                "테스트 AI 응답"
        );

        // when & then 1: 실제 DB 저장 과정에서 예외 발생
        assertThatThrownBy(() ->
                persistenceService.saveCompleted(command)
        )
                .as("DB 컬럼 길이 제약조건을 위반하면 저장에 실패해야 합니다.")
                .isInstanceOf(RuntimeException.class);

        // then 2: 실패한 분석 이력이 DB에 남지 않았는지 확인
        List<AiAnalysis> saved =
                aiAnalysisRepository.findAllByAccountId(accountId);

        assertThat(saved)
                .as("저장 실패 후 분석 이력이 DB에 남아 있으면 안 됩니다.")
                .isEmpty();
    }

}
