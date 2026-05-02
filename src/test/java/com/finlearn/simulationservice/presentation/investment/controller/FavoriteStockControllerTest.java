package com.finlearn.simulationservice.presentation.investment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.GlobalExceptionAdviceImpl;
import com.finlearn.simulationservice.application.investment.dto.request.RegisterFavoriteStockRequest;
import com.finlearn.simulationservice.application.investment.dto.response.FavoriteStockResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FavoriteStockControllerTest {

    @Mock
    private InvestmentService investmentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        FavoriteStockController controller = new FavoriteStockController(investmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionAdviceImpl())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("관심 종목 등록 API는 CommonResponse 포맷으로 응답한다.")
    void registerFavoriteStock() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID favoriteStockId = UUID.randomUUID();
        RegisterFavoriteStockRequest request = new RegisterFavoriteStockRequest(StockAssetType.STOCK, "005930");

        when(investmentService.registerFavoriteStock(eq(userId), any(RegisterFavoriteStockRequest.class)))
                .thenReturn(favoriteStockId);

        mockMvc.perform(post("/api/investments/favorites")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("관심 종목 등록 성공"))
                .andExpect(jsonPath("$.data").value(favoriteStockId.toString()));
    }

    @Test
    @DisplayName("관심 종목 조회 API는 유저 기준 목록을 반환한다.")
    void getFavoriteStocks() throws Exception {
        UUID userId = UUID.randomUUID();
        FavoriteStockResponse first = new FavoriteStockResponse(UUID.randomUUID(), StockAssetType.STOCK, "005930", "삼성전자");
        FavoriteStockResponse second = new FavoriteStockResponse(UUID.randomUUID(), StockAssetType.ETF, "069500", "KODEX 200");

        when(investmentService.getFavoriteStocks(userId)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/investments/favorites")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("관심 종목 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].symbol").value("005930"))
                .andExpect(jsonPath("$.data[0].stockName").value("삼성전자"))
                .andExpect(jsonPath("$.data[1].symbol").value("069500"));
    }

    @Test
    @DisplayName("관심 종목 삭제 API는 CommonResponse 포맷으로 응답한다.")
    void deleteFavoriteStock() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/investments/favorites/005930")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("관심 종목 삭제 성공"));
    }

    @Test
    @DisplayName("중복 등록 예외는 공통 에러 응답 포맷으로 반환된다.")
    void registerDuplicateFavoriteStock() throws Exception {
        UUID userId = UUID.randomUUID();
        RegisterFavoriteStockRequest request = new RegisterFavoriteStockRequest(StockAssetType.STOCK, "005930");
        doThrow(new InvestmentException(InvestmentErrorCode.FAVORITE_STOCK_ALREADY_EXISTS))
                .when(investmentService).registerFavoriteStock(eq(userId), any(RegisterFavoriteStockRequest.class));

        mockMvc.perform(post("/api/investments/favorites")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("이미 등록된 관심 종목입니다."));
    }
}
