package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.investment.dto.request.RegisterFavoriteStockRequest;
import com.finlearn.simulationservice.application.investment.dto.response.FavoriteStockResponse;
import com.finlearn.simulationservice.application.investment.service.InvestmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/investments/favorites")
public class FavoriteStockController {

    private final InvestmentService investmentService;

    @PostMapping
    public CommonResponse<UUID> registerFavoriteStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody RegisterFavoriteStockRequest request
    ) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        UUID userId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        UUID favoriteStockId = investmentService.registerFavoriteStock(userId, request);
        return CommonResponse.success("관심 종목 등록 성공", favoriteStockId);
    }

    @GetMapping
    public CommonResponse<List<FavoriteStockResponse>> getFavoriteStocks(@RequestHeader("X-User-Id") String userIdHeader) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        UUID userId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        return CommonResponse.success("관심 종목 조회 성공", investmentService.getFavoriteStocks(userId));
    }

    @DeleteMapping("/{symbol}")
    public CommonResponse<Void> deleteFavoriteStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String symbol
    ) {
        // TODO: Gateway/JWT 연동 후 X-User-Id 대신 인증 컨텍스트에서 userId를 추출하도록 변경
        UUID userId = InvestmentUserIdHeaderParser.parse(userIdHeader);
        investmentService.deleteFavoriteStock(userId, symbol);
        return CommonResponse.success("관심 종목 삭제 성공", null);
    }
}
