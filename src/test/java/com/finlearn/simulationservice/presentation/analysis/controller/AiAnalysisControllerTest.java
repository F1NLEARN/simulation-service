package com.finlearn.simulationservice.presentation.analysis.controller;

import com.finlearn.common.exception.GlobalExceptionAdviceImpl;
import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisDetailResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisListResponse;
import com.finlearn.simulationservice.application.analysis.service.AiAnalysisQueryService;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisForbiddenException;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiAnalysisControllerTest {

    @Mock
    private AiAnalysisQueryService aiAnalysisQueryService;

    private MockMvc mockMvc;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID AI_ANALYSIS_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AiAnalysisController controller = new AiAnalysisController(aiAnalysisQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionAdviceImpl())
                .build();
    }

    private AiAnalysisListResponse buildListResponse() {
        return new AiAnalysisListResponse(
                AI_ANALYSIS_ID, SEASON_ID, 1,
                new BigDecimal("65.50"), new BigDecimal("80.00"),
                "ETF 분산 투자 전략",
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                LocalDateTime.of(2026, 4, 30, 10, 0),
                AnalysisStatus.READY
        );
    }

    private AiAnalysisDetailResponse buildDetailResponse() {
        return new AiAnalysisDetailResponse(
                AI_ANALYSIS_ID, ACCOUNT_ID, TARGET_USER_ID, "홍길동",
                SEASON_ID, 1,
                new BigDecimal("65.50"), new BigDecimal("80.00"),
                "ETF 분산 투자 전략", "포트폴리오가 기술주에 편중되어 있습니다.",
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                LocalDateTime.of(2026, 4, 30, 10, 0),
                AnalysisStatus.READY
        );
    }

    @Test
    @DisplayName("분석 결과 목록 조회 API는 목록을 반환한다.")
    void getAnalysisList_returnsAnalysisList() throws Exception {
        when(aiAnalysisQueryService.getAnalysisList(ACCOUNT_ID)).thenReturn(List.of(buildListResponse()));

        mockMvc.perform(get("/api/ai-analyses")
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("포트폴리오 분석 결과 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].aiAnalysisId").value(AI_ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$.data[0].seasonNumber").value(1))
                .andExpect(jsonPath("$.data[0].recommendedLearningTopic").value("ETF 분산 투자 전략"))
                .andExpect(jsonPath("$.data[0].analysisStatus").value("READY"));
    }

    @Test
    @DisplayName("최근 분석 결과 조회 API는 상세 정보를 반환한다.")
    void getLatestAnalysis_returnsDetailResponse() throws Exception {
        when(aiAnalysisQueryService.getLatestAnalysis(ACCOUNT_ID)).thenReturn(buildDetailResponse());

        mockMvc.perform(get("/api/ai-analyses/latest")
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("최근 포트폴리오 분석 결과 조회 성공"))
                .andExpect(jsonPath("$.data.aiAnalysisId").value(AI_ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$.data.targetUserName").value("홍길동"))
                .andExpect(jsonPath("$.data.aiFeedbackMessage").value("포트폴리오가 기술주에 편중되어 있습니다."));
    }

    @Test
    @DisplayName("최근 분석 결과가 없으면 404를 반환한다.")
    void getLatestAnalysis_whenNotFound_returns404() throws Exception {
        when(aiAnalysisQueryService.getLatestAnalysis(ACCOUNT_ID))
                .thenThrow(new AiAnalysisNotFoundException());

        mockMvc.perform(get("/api/ai-analyses/latest")
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("포트폴리오 분석 결과를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("분석 결과 상세 조회 API는 모든 필드를 반환한다.")
    void getAnalysisDetail_returnsAllFields() throws Exception {
        when(aiAnalysisQueryService.getAnalysisDetail(ACCOUNT_ID, AI_ANALYSIS_ID))
                .thenReturn(buildDetailResponse());

        mockMvc.perform(get("/api/ai-analyses/{aiAnalysisId}", AI_ANALYSIS_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("포트폴리오 분석 결과 상세 조회 성공"))
                .andExpect(jsonPath("$.data.aiAnalysisId").value(AI_ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.data.targetUserName").value("홍길동"))
                .andExpect(jsonPath("$.data.aiFeedbackMessage").value("포트폴리오가 기술주에 편중되어 있습니다."))
                .andExpect(jsonPath("$.data.analysisStatus").value("READY"));
    }

    @Test
    @DisplayName("존재하지 않는 분석 ID로 상세 조회하면 404를 반환한다.")
    void getAnalysisDetail_whenNotFound_returns404() throws Exception {
        when(aiAnalysisQueryService.getAnalysisDetail(ACCOUNT_ID, AI_ANALYSIS_ID))
                .thenThrow(new AiAnalysisNotFoundException());

        mockMvc.perform(get("/api/ai-analyses/{aiAnalysisId}", AI_ANALYSIS_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("포트폴리오 분석 결과를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("다른 계좌의 분석 결과를 조회하면 403을 반환한다.")
    void getAnalysisDetail_whenForbidden_returns403() throws Exception {
        when(aiAnalysisQueryService.getAnalysisDetail(ACCOUNT_ID, AI_ANALYSIS_ID))
                .thenThrow(new AiAnalysisForbiddenException());

        mockMvc.perform(get("/api/ai-analyses/{aiAnalysisId}", AI_ANALYSIS_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("해당 포트폴리오 분석 결과에 접근할 권한이 없습니다."));
    }
}