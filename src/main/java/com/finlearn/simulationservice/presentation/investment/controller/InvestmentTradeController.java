package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.investment.dto.response.TradeHistoryListResponse;
import com.finlearn.simulationservice.application.investment.service.InvestmentTradeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/investments/trades")
public class InvestmentTradeController {

    private final InvestmentTradeService investmentTradeService;

    @GetMapping
    public CommonResponse<TradeHistoryListResponse> getTradeHistories(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) String tradeType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        UUID userId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        TradeHistoryListResponse response = investmentTradeService.getTradeHistories(userId, stockCode, tradeType, page, size);
        return CommonResponse.success("거래 이력 조회 성공", response);
    }
}
