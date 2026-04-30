package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.investment.dto.response.InvestmentAccountResponse;
import com.finlearn.simulationservice.application.investment.service.InvestmentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/investments/accounts")
public class InvestmentAccountController {

    private final InvestmentAccountService investmentAccountService;

    @PostMapping
    public CommonResponse<InvestmentAccountResponse> createAccount(
            @RequestHeader("X-User-Id") String userId
    ) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        return CommonResponse.success("투자계좌 생성 성공", investmentAccountService.createAccount(userId));
    }

    @GetMapping("/me")
    public CommonResponse<InvestmentAccountResponse> getMyAccount(
            @RequestHeader("X-User-Id") String userId
    ) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        return CommonResponse.success("내 투자계좌 조회 성공", investmentAccountService.getMyAccount(userId));
    }
}
