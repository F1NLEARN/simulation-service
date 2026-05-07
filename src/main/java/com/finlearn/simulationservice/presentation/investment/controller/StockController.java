package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockItemDetailResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockItemResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockPriceResponse;
import com.finlearn.simulationservice.application.investment.service.InvestmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/investments/stocks")
public class StockController {

    private final InvestmentService investmentService;

    @GetMapping
    public CommonResponse<List<StockItemResponse>> getStockItems(
            @RequestParam(required = false) String assetType
    ) {
        return CommonResponse.success("종목 목록 조회 성공", investmentService.getStockItems(assetType));
    }

    @GetMapping("/{stockCode}")
    public CommonResponse<StockItemDetailResponse> getStockItemDetail(@PathVariable String stockCode) {
        return CommonResponse.success("종목 상세 조회 성공", investmentService.getStockItemDetail(stockCode));
    }

    @GetMapping("/{stockCode}/price")
    public CommonResponse<StockPriceResponse> getCurrentStockPrice(@PathVariable String stockCode) {
        return CommonResponse.success("현재가 조회 성공", investmentService.getCurrentStockPrice(stockCode));
    }
}
