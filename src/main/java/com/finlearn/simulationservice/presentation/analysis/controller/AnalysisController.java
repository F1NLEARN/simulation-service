package com.finlearn.simulationservice.presentation.analysis.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.application.analysis.service.PortfolioAnalysisQueryService;
import com.finlearn.simulationservice.presentation.investment.controller.InvestmentUserIdHeaderParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final PortfolioAnalysisQueryService portfolioAnalysisQueryService;

    // CI/CD 테스트 주석
    @GetMapping("/portfolio")
    public CommonResponse<PortfolioAnalysisResponse> getPortfolioAnalysis(
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        UUID investorId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        GetPortfolioAnalysisQuery query = new GetPortfolioAnalysisQuery(investorId);
        return CommonResponse.success("포트폴리오 분석 조회 성공", portfolioAnalysisQueryService.getPortfolioAnalysis(query));
    }
}
