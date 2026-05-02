package com.finlearn.simulationservice.presentation.tradehistory.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryDetailResponse;
import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryListResponse;
import com.finlearn.simulationservice.application.tradehistory.query.GetTradeHistoryListQuery;
import com.finlearn.simulationservice.application.tradehistory.service.TradeHistoryQueryService;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade-histories")
public class TradeHistoryController {

    private final TradeHistoryQueryService tradeHistoryQueryService;

    @GetMapping
    public CommonResponse<Page<TradeHistoryListResponse>> getTradeHistoryList(
            @RequestHeader("X-Account-Id") UUID accountId,
            @RequestParam(required = false) TradeType tradeType,
            @RequestParam(required = false) TradeStatus status,
            @RequestParam(required = false) String instrumentCode,
            @PageableDefault(size = 20, sort = "tradeAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(
                accountId, tradeType, status, instrumentCode, pageable
        );
        return CommonResponse.success("거래 내역 목록 조회 성공", tradeHistoryQueryService.getTradeHistoryList(query));
    }

    @GetMapping("/{tradeHistoryId}")
    public CommonResponse<TradeHistoryDetailResponse> getTradeHistoryDetail(
            @RequestHeader("X-Account-Id") UUID accountId,
            @PathVariable UUID tradeHistoryId
    ) {
        return CommonResponse.success("거래 내역 상세 조회 성공",
                tradeHistoryQueryService.getTradeHistoryDetail(accountId, tradeHistoryId));
    }
}