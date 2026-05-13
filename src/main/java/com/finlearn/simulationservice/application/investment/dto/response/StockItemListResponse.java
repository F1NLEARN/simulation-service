package com.finlearn.simulationservice.application.investment.dto.response;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import java.util.List;
import org.springframework.data.domain.Page;

public record StockItemListResponse(
        List<StockItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static StockItemListResponse from(Page<StockItem> stockItems) {
        return new StockItemListResponse(
                stockItems.getContent().stream()
                        .map(StockItemResponse::from)
                        .toList(),
                stockItems.getNumber(),
                stockItems.getSize(),
                stockItems.getTotalElements(),
                stockItems.getTotalPages(),
                stockItems.hasNext()
        );
    }
}
