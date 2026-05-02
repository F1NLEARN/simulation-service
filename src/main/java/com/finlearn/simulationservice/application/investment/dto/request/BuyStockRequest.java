package com.finlearn.simulationservice.application.investment.dto.request;

import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record BuyStockRequest(
        @NotNull(message = "투자계좌 ID는 필수입니다.")
        UUID accountId,

        String instrumentCode,

        @NotNull(message = "자산 유형은 필수입니다.")
        StockAssetType assetType,
        @NotBlank(message = "종목 코드는 필수입니다.")
        String symbol,
        @Positive(message = "주문 수량은 1 이상이어야 합니다.")
        long quantity
) {
}
