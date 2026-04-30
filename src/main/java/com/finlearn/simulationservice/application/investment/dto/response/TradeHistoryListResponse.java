package com.finlearn.simulationservice.application.investment.dto.response;

import java.util.List;

public record TradeHistoryListResponse(
        List<TradeHistoryResponse> trades,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
