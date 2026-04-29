package com.finlearn.simulationservice.application.investment.dto.request;

import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterFavoriteStockRequest(
        @NotNull(message = "자산 유형은 필수입니다.")
        StockAssetType assetType,
        @NotBlank(message = "종목 코드는 필수입니다.")
        String symbol
) {
}
