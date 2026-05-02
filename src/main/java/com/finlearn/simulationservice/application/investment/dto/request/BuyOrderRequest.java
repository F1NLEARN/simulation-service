package com.finlearn.simulationservice.application.investment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record BuyOrderRequest(
        @NotBlank(message = "종목 코드는 필수입니다.")
        String stockCode,
        @Positive(message = "주문 수량은 1 이상이어야 합니다.")
        long quantity
) {
}
