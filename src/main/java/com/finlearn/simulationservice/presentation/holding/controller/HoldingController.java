package com.finlearn.simulationservice.presentation.holding.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.simulationservice.application.holding.dto.response.HoldingDetailResponse;
import com.finlearn.simulationservice.application.holding.dto.response.HoldingListResponse;
import com.finlearn.simulationservice.application.holding.query.GetHoldingListQuery;
import com.finlearn.simulationservice.application.holding.service.HoldingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingQueryService holdingQueryService;

    @GetMapping
    public CommonResponse<List<HoldingListResponse>> getHoldingList(
            @RequestHeader("X-Account-Id") UUID accountId,
            @RequestParam(required = false) String instrumentCode
    ) {
        GetHoldingListQuery query = new GetHoldingListQuery(accountId, instrumentCode);
        return CommonResponse.success("보유 종목 목록 조회 성공", holdingQueryService.getHoldingList(query));
    }

    @GetMapping("/{holdingId}")
    public CommonResponse<HoldingDetailResponse> getHoldingDetail(
            @RequestHeader("X-Account-Id") UUID accountId,
            @PathVariable UUID holdingId
    ) {
        return CommonResponse.success("보유 종목 상세 조회 성공",
                holdingQueryService.getHoldingDetail(accountId, holdingId));
    }
}
