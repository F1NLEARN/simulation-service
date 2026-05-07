package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.investment.dto.response.HoldingResponse;
import com.finlearn.simulationservice.application.investment.service.InvestmentHoldingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/investments/holdings")
public class InvestmentHoldingController {

    private final InvestmentHoldingService investmentHoldingService;

    @GetMapping
    public CommonResponse<List<HoldingResponse>> getMyHoldings(@RequestHeader("X-User-Id") String userIdHeader) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        UUID userId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        return CommonResponse.success("보유 종목 조회 성공", investmentHoldingService.getMyHoldings(userId));
    }
}
