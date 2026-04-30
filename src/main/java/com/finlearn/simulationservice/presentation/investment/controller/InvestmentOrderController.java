package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.investment.dto.request.BuyOrderRequest;
import com.finlearn.simulationservice.application.investment.dto.request.SellOrderRequest;
import com.finlearn.simulationservice.application.investment.dto.response.BuyStockResponse;
import com.finlearn.simulationservice.application.investment.dto.response.SellStockResponse;
import com.finlearn.simulationservice.application.investment.service.InvestmentOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/investments/orders")
public class InvestmentOrderController {

    private final InvestmentOrderService investmentOrderService;

    @PostMapping("/buy")
    public CommonResponse<BuyStockResponse> buy(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody BuyOrderRequest request
    ) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        return CommonResponse.success("매수 주문 성공", investmentOrderService.buy(userId, request));
    }

    @PostMapping("/sell")
    public CommonResponse<SellStockResponse> sell(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SellOrderRequest request
    ) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        return CommonResponse.success("매도 주문 성공", investmentOrderService.sell(userId, request));
    }
}
