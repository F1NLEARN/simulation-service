package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisType;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiAnalysisPersistenceServiceTest {

    @Mock
    private AiAnalysisRepository aiAnalysisRepository;

    private AiAnalysisPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService =
                new AiAnalysisPersistenceService(aiAnalysisRepository);
    }

    private CreateAiAnalysisCommand createCommand() {
        LocalDateTime now = LocalDateTime.now();

        return new CreateAiAnalysisCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스터",
                UUID.randomUUID(),
                1,
                AnalysisType.PORTFOLIO,
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(86),
                "DOMESTIC_ETF",
                "포트폴리오 분석 요약",
                "AI 추천 결과",
                now.toLocalDate().atStartOfDay(),
                now,
                now,
                "테스트 프롬프트",
                "테스트 AI 응답"
        );
    }

    @Test
    @DisplayName("정상 분석 이력을 COMPLETED 상태로 저장한다.")
    void saveCompleted_savesCompletedAnalysis() {
        persistenceService.saveCompleted(createCommand());

        ArgumentCaptor<AiAnalysis> captor =
                ArgumentCaptor.forClass(AiAnalysis.class);

        verify(aiAnalysisRepository).save(captor.capture());

        AiAnalysis saved = captor.getValue();

        assertThat(saved.getAnalysisStatus())
                .isEqualTo(AnalysisStatus.COMPLETED);

        assertThat(saved.getFailureReason())
                .isNull();

        assertThat(saved.getAiFeedbackMessage())
                .isEqualTo("AI 추천 결과");
    }

    @Test
    @DisplayName("실패 분석 이력을 FAILED 상태로 저장한다.")
    void saveFailed_savesFailedAnalysis() {
        persistenceService.saveFailed(
                createCommand(),
                "AI API timeout"
        );

        ArgumentCaptor<AiAnalysis> captor =
                ArgumentCaptor.forClass(AiAnalysis.class);

        verify(aiAnalysisRepository).save(captor.capture());

        AiAnalysis saved = captor.getValue();

        assertThat(saved.getAnalysisStatus())
                .isEqualTo(AnalysisStatus.FAILED);

        assertThat(saved.getFailureReason())
                .isEqualTo("AI API timeout");
    }
}
