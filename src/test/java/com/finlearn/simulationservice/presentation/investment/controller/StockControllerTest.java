package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.common.exception.GlobalExceptionAdviceImpl;
import com.finlearn.simulationservice.application.investment.dto.response.StockItemDetailResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockItemResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockPriceResponse;
import com.finlearn.simulationservice.application.investment.service.InvestmentService;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private InvestmentService investmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StockController controller = new StockController(investmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionAdviceImpl())
                .build();
    }

    @Test
    @DisplayName("종목 목록 조회 API는 종목명/종목코드/자산유형을 반환한다.")
    void getStockItems() throws Exception {
        StockItemResponse first = new StockItemResponse(
                UUID.randomUUID(),
                "005930",
                "삼성전자",
                StockAssetType.STOCK,
                73500L,
                true
        );
        StockItemResponse second = new StockItemResponse(
                UUID.randomUUID(),
                "069500",
                "KODEX 200",
                StockAssetType.ETF,
                35000L,
                true
        );
        when(investmentService.getStockItems(null)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/investments/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("종목 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].name").value("삼성전자"))
                .andExpect(jsonPath("$.data[0].stockCode").value("005930"))
                .andExpect(jsonPath("$.data[0].assetType").value("STOCK"))
                .andExpect(jsonPath("$.data[0].currentPrice").value(73500))
                .andExpect(jsonPath("$.data[0].tradable").value(true))
                .andExpect(jsonPath("$.data[1].assetType").value("ETF"));
    }

    @Test
    @DisplayName("지원하지 않는 자산유형이면 공통 예외 포맷으로 반환한다.")
    void getStockItemsFailWhenInvalidAssetType() throws Exception {
        doThrow(new InvestmentException(InvestmentErrorCode.INVALID_ASSET_TYPE))
                .when(investmentService).getStockItems("CRYPTO");

        mockMvc.perform(get("/api/v1/investments/stocks").param("assetType", "CRYPTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("지원하지 않는 자산 유형입니다."));
    }

    @Test
    @DisplayName("종목 상세 조회 API는 매수/매도에 사용할 현재가 정보를 포함한다.")
    void getStockItemDetail() throws Exception {
        StockItemDetailResponse response = new StockItemDetailResponse(
                UUID.randomUUID(),
                "005930",
                "삼성전자",
                StockAssetType.STOCK,
                73500L,
                true
        );
        when(investmentService.getStockItemDetail("005930")).thenReturn(response);

        mockMvc.perform(get("/api/v1/investments/stocks/005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("종목 상세 조회 성공"))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.name").value("삼성전자"))
                .andExpect(jsonPath("$.data.currentPrice").value(73500))
                .andExpect(jsonPath("$.data.tradable").value(true));
    }

    @Test
    @DisplayName("없는 종목 코드로 상세 조회 시 공통 예외 포맷으로 반환한다.")
    void getStockItemDetailFailWhenNotFound() throws Exception {
        doThrow(new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND))
                .when(investmentService).getStockItemDetail("999999");

        mockMvc.perform(get("/api/v1/investments/stocks/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("종목을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("현재가 조회 API는 종목코드 기준 현재가를 반환한다.")
    void getCurrentStockPrice() throws Exception {
        when(investmentService.getCurrentStockPrice("005930"))
                .thenReturn(new StockPriceResponse("005930", 73500L));

        mockMvc.perform(get("/api/v1/investments/stocks/005930/price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("현재가 조회 성공"))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.currentPrice").value(73500));
    }

    @Test
    @DisplayName("현재가가 없으면 공통 예외 포맷으로 반환한다.")
    void getCurrentStockPriceFailWhenNotFound() throws Exception {
        doThrow(new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND))
                .when(investmentService).getCurrentStockPrice("999999");

        mockMvc.perform(get("/api/v1/investments/stocks/999999/price"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("현재 시세를 찾을 수 없습니다."));
    }
}
