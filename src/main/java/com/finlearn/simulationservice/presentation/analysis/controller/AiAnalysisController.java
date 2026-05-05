package com.finlearn.simulationservice.presentation.analysis.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisDetailResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisListResponse;
import com.finlearn.simulationservice.application.analysis.service.AiAnalysisQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai-analyses")
public class AiAnalysisController {

    private final AiAnalysisQueryService aiAnalysisQueryService;

    @GetMapping
    public CommonResponse<List<AiAnalysisListResponse>> getAnalysisList(
            @RequestHeader("X-Account-Id") UUID accountId
    ) {
        return CommonResponse.success("포트폴리오 분석 결과 목록 조회 성공",
                aiAnalysisQueryService.getAnalysisList(accountId));
    }

    @GetMapping("/latest")
    public CommonResponse<AiAnalysisDetailResponse> getLatestAnalysis(
            @RequestHeader("X-Account-Id") UUID accountId
    ) {
        return CommonResponse.success("최근 포트폴리오 분석 결과 조회 성공",
                aiAnalysisQueryService.getLatestAnalysis(accountId));
    }

    @GetMapping("/{aiAnalysisId}")
    public CommonResponse<AiAnalysisDetailResponse> getAnalysisDetail(
            @RequestHeader("X-Account-Id") UUID accountId,
            @PathVariable UUID aiAnalysisId
    ) {
        return CommonResponse.success("포트폴리오 분석 결과 상세 조회 성공",
                aiAnalysisQueryService.getAnalysisDetail(accountId, aiAnalysisId));
    }
}