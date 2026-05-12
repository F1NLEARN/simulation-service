package com.finlearn.simulationservice.presentation.analysis.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisHistoryResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetAiAnalysisHistoryQuery;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.application.analysis.service.AiAnalysisHistoryQueryService;
import com.finlearn.simulationservice.application.analysis.service.PortfolioAnalysisQueryService;
import com.finlearn.simulationservice.presentation.investment.controller.InvestmentUserIdHeaderParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final PortfolioAnalysisQueryService portfolioAnalysisQueryService;

    @GetMapping("/portfolio")
    public CommonResponse<PortfolioAnalysisResponse> getPortfolioAnalysis(
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        UUID investorId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        GetPortfolioAnalysisQuery query = new GetPortfolioAnalysisQuery(investorId);
        return CommonResponse.success("포트폴리오 분석 조회 성공", portfolioAnalysisQueryService.getPortfolioAnalysis(query));
        return CommonResponse.success("포트폴리오 분석 조회 성공",
                portfolioAnalysisQueryService.getPortfolioAnalysis(new GetPortfolioAnalysisQuery(investorId)));
    }

    @PostMapping("/portfolio/refresh")
    public CommonResponse<PortfolioAnalysisResponse> refreshPortfolioAnalysis(
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        UUID investorId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        portfolioAnalysisQueryService.evictCache(investorId);
        PortfolioAnalysisResponse response = portfolioAnalysisQueryService
                .getPortfolioAnalysis(new GetPortfolioAnalysisQuery(investorId));
        return CommonResponse.success("포트폴리오 분석 갱신 성공", response);
    }
    }
}
